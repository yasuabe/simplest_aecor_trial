package simplest.sum.writeside

import cats.effect.ExitCode
import cats.syntax.functor._
import monix.eval.{Task, TaskApp}
import simplest.sum.infra.{SumPgJournal, UsingActorSystem}
import simplest.sum.writeside.infra.SumRuntime
import simplest.sum.model.runtime.SumKey

object ZeroMain extends TaskApp with UsingActorSystem {
  def run(args: List[String]): Task[ExitCode] = actorSystem("sum") use { s =>
    val journal = SumPgJournal.eventJournal[Task]
    (for {
      _    <- journal.createTable          // ジャーナルテーブルがなければ作る
      sums <- SumRuntime.sums(s, journal)
      _    <- sums(SumKey.random()).create
    } yield ()) as ExitCode.Success
  }
}
