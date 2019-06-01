package simplest.readside

import scala.io.StdIn
import aecor.distributedprocessing.DistributedProcessing
import aecor.distributedprocessing.DistributedProcessing.Process
import cats.effect.{Concurrent, ContextShift, ExitCode, Timer}
import monix.eval.{Task, TaskApp}
import simplest.infra._
import simplest.readside.impl.{IncrementViewRepoImpl, IncrementProjectionImpl, projectionSink}
import simplest.util.UsingActorSystem

object ReaderMain extends TaskApp with UsingActorSystem {
  def processes[F[_]: Timer: Concurrent: ContextShift](journal: PostgresJournal[F]): List[Process[F]] = {
    val projection = new IncrementProjectionImpl[F](new IncrementViewRepoImpl[F](journal.transactor))
    journal.createProcesses(projectionSink(projection))
  }

  def run(args: List[String]): Task[ExitCode] = {
    import cats.effect._
    import cats.implicits._
    import cats.temp.par._
    import doobie.syntax.connectionio._
    actorSystem("increment") use { actorSys =>
      val j = new PostgresJournal[Task]
      for {
        _ <-  List(j.journal.createTable, j.offsetStore.createTable.transact(j.transactor)).parSequence
        killSwitch <- DistributedProcessing(actorSys).start("ViewProjectionProcessing", processes[Task](j))
        _ <- Task {
          StdIn.readLine("press [ENTER]")
        } >> killSwitch.shutdown
      } yield ExitCode.Success
    }
  }
}
