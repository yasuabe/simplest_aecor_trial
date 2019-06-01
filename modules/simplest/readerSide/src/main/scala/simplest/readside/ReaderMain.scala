package simplest.readside

import aecor.data.{Committable, ConsumerId, EventTag, TagConsumer}

import scala.io.StdIn
import aecor.distributedprocessing.DistributedProcessing
import aecor.distributedprocessing.DistributedProcessing.Process
import aecor.journal.postgres.{Offset, PostgresOffsetStore}
import aecor.runtime.KeyValueStore
import cats.effect.{Async, Concurrent, ContextShift, ExitCode, Timer}
import doobie.util.transactor.Transactor
import monix.eval.{Task, TaskApp}
import simplest.infra._
import simplest.readside.impl.{IncrementProjectionImpl, projectionSink}
import simplest.util.{UsingActorSystem, streamToProcess}

import scala.concurrent.duration._

object ReaderMain extends TaskApp with UsingActorSystem {
  def createProcesses[F[_]](
    journal: IncrementJournal[F],
    offsetStore: KeyValueStore[F, TagConsumer, Offset],
    sink: fs2.Sink[F, Committable[F, IncrementEntityEvent]]
  )(implicit F: Concurrent[F], G: Timer[F]): List[Process[F]] = {
    val queries = journal.queries(100.millis).withOffsetStore(offsetStore)

    def tagStream(tag: EventTag): fs2.Stream[F, Unit] = {
      val eventStream = queries.eventsByTag(tag, ConsumerId("ViewProjection"))
      fs2.Stream
        .force(eventStream)
        .map(_.map { case (_, event) => event })
        .through(sink)
    }
    PostgresJournal.tagging.tags.map(t => streamToProcess(tagStream(t)))
  }
  lazy val offsetStoreCIO = PostgresOffsetStore("consumer_offset")
  def offsetStore[F[_]: Async](transactor: Transactor[F]): KeyValueStore[F, TagConsumer, Offset] =
    offsetStoreCIO mapK transactor.trans

  def processes[F[_]: Timer: Concurrent: ContextShift](journal: PostgresJournal[F]): List[Process[F]] = {
    val projection = new IncrementProjectionImpl[F] // TODO projectionSink を一体化できないか
    createProcesses(journal.journal, offsetStore(journal.transactor), projectionSink(projection))
  }

  def run(args: List[String]): Task[ExitCode] = {
    import cats.effect._
    import cats.implicits._
    import cats.temp.par._
    import doobie.syntax.connectionio._
    actorSystem("increment") use { actorSys =>
      val j = new PostgresJournal[Task] // TODO 変数名
      for {
        _ <-  List(j.journal.createTable, offsetStoreCIO.createTable.transact(j.transactor)).parSequence // TODO 初期化プロセスまとめる
        killSwitch <- DistributedProcessing(actorSys).start("ViewProjectionProcessing", processes[Task](j))
        _ <- Task {
          StdIn.readLine("press [ENTER]")
        } >> killSwitch.shutdown
      } yield ExitCode.Success
    }
  }
}
