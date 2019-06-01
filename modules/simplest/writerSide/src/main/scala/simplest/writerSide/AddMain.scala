package simplest.writerSide

import cats.effect.ExitCode
import cats.syntax.functor._
import monix.eval.{Task, TaskApp}
import simplest.infra._
import simplest.util.UsingActorSystem
import simplest.writerSide.infra._

object AddMain extends TaskApp with UsingActorSystem {
  def run(args: List[String]): Task[ExitCode] = args.headOption match {
    case None     => Task(ExitCode.Error)
    case Some(id) => actorSystem("increment") use { actorSys =>
      (for {
        journal    <- PostgresJournal[Task].map(_.journal)
        increments <- IncrementsRuntime.increments(actorSys, journal)
        _          <- increments(IncrementKey(id)).add(100)
      } yield ()) as ExitCode.Success
    }
  }
}
