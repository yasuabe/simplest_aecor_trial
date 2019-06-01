package simplest.writerSide

import cats.effect.ExitCode
import cats.syntax.functor._
import doobie.util.transactor.Transactor
import monix.eval.{Task, TaskApp}
import simplest.infra._
import simplest.util.UsingActorSystem
import simplest.writerSide.infra._

object CreateMain extends TaskApp with UsingActorSystem {
  lazy val transactor: Transactor[Task] = Transactor.fromDriverManager[Task](
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/increment",
    "postgres",
    ""
  )
  def run(args: List[String]): Task[ExitCode] =
    actorSystem("increment") use { s =>
      val journal = PostgresJournal.eventJournal[Task](transactor)
      (for {
        _          <- journal.createTable
        increments <- IncrementsRuntime.increments(s, journal)
        _          <- increments(IncrementKey.random()).create
      } yield ()) as ExitCode.Success
    }
}
