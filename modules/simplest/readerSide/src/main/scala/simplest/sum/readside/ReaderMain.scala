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
import simplest.sum.infra.{PostgresJournal, UsingActorSystem}
import simplest.sum.util.streamToProcess
import simplest.sum.readside.infra.SumProjection

import scala.concurrent.duration._
import scala.io.StdIn

trait ReaderProgram[F[_]] {
  import PostgresJournal._

  implicit val F: Concurrent[F]
  implicit val T: Timer[F]
  implicit val X: ContextShift[F]

  private val offsetStoreCIO  = PostgresOffsetStore("consumer_offset")
  private lazy val projection = new SumProjection

  private def offsetStore(t: Transactor[F]) = offsetStoreCIO mapK t.trans

  def processes: List[Process[F]] = {
    val queries = eventJournal.queries(100.millis).withOffsetStore(offsetStore(transactor))

    def tagStream(tag: EventTag): fs2.Stream[F, Unit] = {
      val eventStream = queries.eventsByTag(tag, ConsumerId("ViewProjection"))
      fs2.Stream
        .force(eventStream)
        .map(_.map { case (_, event) => event })
        .through(projection.sink)
    }
    tagging.tags.map(t => streamToProcess(tagStream(t)))
  }
  def prepareTables: List[F[Unit]] = List(
    eventJournal.createTable,
    offsetStoreCIO.createTable.transact(transactor)
  )
}
object ReaderMain extends TaskApp with ReaderProgram[Task] with UsingActorSystem {
  lazy val F: Concurrent[Task]   = implicitly[Concurrent[Task]]
  lazy val T: Timer[Task]        = implicitly[Timer[Task]]
  lazy val X: ContextShift[Task] = implicitly[ContextShift[Task]]

  def run(args: List[String]): Task[ExitCode] = actorSystem("sum") use { sys =>
    for {
      _          <- prepareTables.parSequence
      killSwitch <- DistributedProcessing(sys).start("ViewProjectionProcessing", processes)
      _          <- Task { StdIn.readLine("press [ENTER]") } >> killSwitch.shutdown
    } yield ExitCode.Success
  }
}
