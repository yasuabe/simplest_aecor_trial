package simplest.sum.writeside

import cats.effect.ExitCode
import cats.syntax.functor._
import monix.eval.{Task, TaskApp}
import simplest.sum.infra.{PostgresJournal, SumKey, UsingActorSystem}
import simplest.sum.writeside.infra.SumRuntime

object ZeroMain extends TaskApp with UsingActorSystem {
  def run(args: List[String]): Task[ExitCode] = actorSystem("sum") use { s =>
    val journal = PostgresJournal.eventJournal[Task]
    (for {
      _    <- journal.createTable          // ジャーナルテーブルがなければ作る
      sums <- SumRuntime.sums(s, journal)
      _    <- sums(SumKey.random()).create
    } yield ()) as ExitCode.Success
  }
}
