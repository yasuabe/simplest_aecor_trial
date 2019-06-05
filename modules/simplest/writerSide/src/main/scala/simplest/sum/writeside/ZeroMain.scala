package simplest.sum.writeside

import cats.effect.ExitCode
import cats.syntax.functor._
import doobie.util.transactor.Transactor
import monix.eval.{Task, TaskApp}
import simplest.sum.infra.SumKey
import simplest.sum.infra.PostgresJournal
import simplest.sum.util.UsingActorSystem
import simplest.sum.writeside.infra.SumRuntime

object ZeroMain extends TaskApp with UsingActorSystem {
  lazy val transactor: Transactor[Task] = Transactor.fromDriverManager[Task](
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/sumdb",
    "postgres",
    ""
  )
  def run(args: List[String]): Task[ExitCode] =
    actorSystem("sum") use { s =>
      val journal = PostgresJournal.eventJournal[Task](transactor)
      (for {
        _    <- journal.createTable
        sums <- SumRuntime.sums(s, journal)
        _    <- sums(SumKey.random()).create
      } yield ()) as ExitCode.Success
    }
}
