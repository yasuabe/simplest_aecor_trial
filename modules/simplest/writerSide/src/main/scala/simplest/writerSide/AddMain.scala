package simplest.writerSide

import cats.effect.ExitCode
import cats.syntax.functor._
import doobie.util.transactor.Transactor
import monix.eval.{Task, TaskApp}
import simplest.infra._
import simplest.util.UsingActorSystem
import simplest.writerSide.infra._

object AddMain extends TaskApp with UsingActorSystem {
  lazy val transactor: Transactor[Task] = Transactor.fromDriverManager[Task](
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/increment",
    "postgres",
    ""
  )
  def run(args: List[String]): Task[ExitCode] = args.headOption match {
    case None     => Task(ExitCode.Error)
    case Some(id) => actorSystem("increment") use { actorSys =>
      val journal = PostgresJournal.eventJournal[Task](transactor)
      (for {
        _          <- journal.createTable
        increments <- IncrementsRuntime.increments(actorSys, journal)
        _          <- increments(IncrementKey(id)).add(100)
      } yield ()) as ExitCode.Success
    }
  }
}
