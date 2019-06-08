package simplest.sum.writeside

import cats.effect.ExitCode
import cats.syntax.functor._
import monix.eval.{Task, TaskApp}
import simplest.sum.infra.{SumPgJournal, UsesTransactor, UsingActorSystem}
import simplest.sum.model.runtime.SumKey
import simplest.sum.writeside.infra.SumRuntime

import scala.util.Try

object AddMain extends TaskApp with UsingActorSystem with UsesTransactor[Task] {
  def run(args: List[String]): Task[ExitCode] = parseArgs(args) match {
    case Some((key, n)) => actorSystem("sum") use { actorSys =>
      val journal = SumPgJournal.journal[Task](transactor)
      (for {
        _    <- journal.createTable
        sums <- SumRuntime.sums(actorSys, journal)
        _    <- sums(key).add(n)
      } yield ()) as ExitCode.Success
    }
    case _ => Task(ExitCode.Error)
  }
  private def parseArgs(args: List[String]): Option[(SumKey, Int)] = args match {
    case id :: num :: Nil => Try(num.toInt).toOption.map((SumKey(id), _))
    case _                => None
  }
}
