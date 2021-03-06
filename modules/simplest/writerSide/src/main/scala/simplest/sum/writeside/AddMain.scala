package simplest.sum.writeside

import akka.actor.ActorSystem
import cats.effect.ExitCode
import cats.syntax.functor._
import monix.eval.{Task, TaskApp}
import simplest.sum.infra.{SumPgJournal, UsesActorSystem, UsesTransactor}
import simplest.sum.model.runtime.SumKey
import simplest.sum.writeside.infra.SumRuntime

import scala.util.Try

object AddMain extends TaskApp with UsesActorSystem with UsesTransactor[Task] {
  private def parseArgs(args: List[String]): Option[(SumKey, Int)] = args match {
    case id :: num :: Nil => Try(num.toInt).toOption.map((SumKey(id), _))
    case _                => None
  }
  def program(n: Int, key: SumKey)(sys: ActorSystem): Task[Unit] = {
    val journal = SumPgJournal.journal[Task](transactor)
    for {
      _    <- journal.createTable
      sums <- SumRuntime.sums(sys, journal)
      _    <- sums(key).add(n)
    } yield ()
  }
  def run(args: List[String]): Task[ExitCode] = parseArgs(args) match {
    case Some((key, n)) => actorSystem("sum").use(program(n, key)).as(ExitCode.Success)
    case _              => Task(ExitCode.Error)
  }
}
