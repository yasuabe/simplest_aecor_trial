package simplest.util

import akka.actor.ActorSystem
import cats.effect.{Async, IO, Resource}
import com.typesafe.config.ConfigFactory
import cats.syntax.functor._

trait UsingActorSystem {
  def actorSystem[F[_]](name: String)(implicit F: Async[F]): Resource[F, ActorSystem] = for {
    config  <- Resource.liftF(F.delay(ConfigFactory.load()))
    acquire =  F.delay(ActorSystem(name, config))
    release =  (s: ActorSystem) => IO.fromFuture(IO(s.terminate())).to[F].void
    sys     <- Resource.make(acquire)(release)
  } yield sys
}
//trait UsingActorSystem {
//  def actorSystem(name: String): Resource[Task, ActorSystem] = for {
//    //TODO: Resource.liftF(Task.delay(ConfigFactory.load()))
//    config <- Resource.liftF(Task.delay(ConfigFactory.load()))
//    system <- Resource.make(Task(ActorSystem(name, config)))(
//      s => Task(Task.fromFuture(s.terminate())).flatten.void
//    )
//  } yield system
//}
