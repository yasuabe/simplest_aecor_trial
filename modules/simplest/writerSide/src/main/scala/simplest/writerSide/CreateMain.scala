package simplest.writerSide

import cats.effect.ExitCode
import cats.syntax.functor._
import monix.eval.{Task, TaskApp}
import simplest.infra._
import simplest.util.UsingActorSystem
import simplest.writerSide.infra._

object CreateMain extends TaskApp with UsingActorSystem {
  def run(args: List[String]): Task[ExitCode] =
    actorSystem("increment") use { s =>
      (for {
        journal    <- PostgresJournal[Task].map(_.journal)
        increments <- IncrementsRuntime.increments(s, journal)
        _          <- increments(IncrementKey.random()).create
      } yield ()) as ExitCode.Success
    }
}
