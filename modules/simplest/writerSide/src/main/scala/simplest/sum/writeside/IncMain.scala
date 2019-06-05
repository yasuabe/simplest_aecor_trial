package simplest.sum.writeside

import cats.effect.ExitCode
import cats.syntax.functor._
import doobie.util.transactor.Transactor
import monix.eval.{Task, TaskApp}
import simplest.sum.infra.{PostgresJournal, SumKey}
import simplest.sum.util.UsingActorSystem
import simplest.sum.writeside.infra.SumRuntime

object IncMain extends TaskApp with UsingActorSystem {
  lazy val transactor: Transactor[Task] = Transactor.fromDriverManager[Task](
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/sumdb",
    "postgres",
    ""
  )
  def run(args: List[String]): Task[ExitCode] = args.headOption match {
    case None     => Task(ExitCode.Error)
    case Some(id) => actorSystem("sum") use { actorSys =>
      val journal = PostgresJournal.eventJournal[Task](transactor)
      (for {
        _    <- journal.createTable
        sums <- SumRuntime.sums(actorSys, journal)
        _    <- sums(SumKey(id)).add(1)
      } yield ()) as ExitCode.Success
    }
  }
}
