package simplest.sum.writeside

import akka.actor.ActorSystem
import cats.effect.ExitCode
import cats.syntax.functor._
import monix.eval.{Task, TaskApp}
import simplest.sum.infra.{SumPgJournal, UsesActorSystem, UsesTransactor}
import simplest.sum.writeside.infra.SumRuntime
import simplest.sum.model.runtime.SumKey

object ZeroMain extends TaskApp with UsesActorSystem with UsesTransactor[Task] {
  def program(sys: ActorSystem): Task[Unit] = {
    val journal = SumPgJournal.journal[Task](transactor)
    for {
      _    <- journal.createTable          // ジャーナルテーブルがなければ作る
      sums <- SumRuntime.sums(sys, journal)
      _    <- sums(SumKey.random()).create
    } yield ()
  }
  def run(args: List[String]): Task[ExitCode] =
    actorSystem("sum") use program as ExitCode.Success
}
