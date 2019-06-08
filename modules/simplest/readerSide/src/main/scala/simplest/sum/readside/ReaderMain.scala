package simplest.sum.readside

import cats.syntax.flatMap._
import cats.syntax.parallel._
import cats.instances.list._
import cats.effect._
import monix.eval.{Task, TaskApp}
import doobie.util.transactor.Transactor
import doobie.syntax.connectionio._
import aecor.data.{ConsumerId, EventTag}
import aecor.distributedprocessing.DistributedProcessing
import aecor.distributedprocessing.DistributedProcessing.Process
import aecor.journal.postgres.PostgresOffsetStore
import simplest.sum.infra.{SumPgJournal, UsingActorSystem}
import simplest.sum.infra._
import simplest.sum.readside.infra.SumProjection

import scala.concurrent.duration._
import scala.io.StdIn

object ReaderMain extends TaskApp with UsingActorSystem with UsesTransactor[Task] {
  private val offsetStoreCIO = PostgresOffsetStore("consumer_offset")
  private lazy val journal   = SumPgJournal.journal[Task](transactor)

  private def offsetStore(t: Transactor[Task]) = offsetStoreCIO mapK t.trans

  def processes: List[Process[Task]] = {
    val queries = journal.queries(100.millis).withOffsetStore(offsetStore(transactor))

    def tagStream(tag: EventTag): fs2.Stream[Task, Unit] = {
      val eventStream = queries.eventsByTag(tag, ConsumerId("ViewProjection"))
      fs2.Stream
        .force(eventStream)
        .map(_.map { case (_, event) => event })
        .through(SumProjection.sink)
    }
    SumPgJournal.tagging.tags.map(tagStream(_).toProcess)
  }
  def prepareTables: List[Task[Unit]] = List(
    journal.createTable,
    offsetStoreCIO.createTable.transact(transactor)
  )
  def run(args: List[String]): Task[ExitCode] = actorSystem("sum") use { sys =>
    for {
      _          <- prepareTables.parSequence
      killSwitch <- DistributedProcessing(sys).start("ViewProjectionProcessing", processes)
      _          <- Task { StdIn.readLine("press [ENTER]") } >> killSwitch.shutdown
    } yield ExitCode.Success
  }
}
