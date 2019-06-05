package simplest.sum.infra

import akka.actor.ActorSystem
import cats.effect.{Async, IO, Resource}
import cats.syntax.functor._
import com.typesafe.config.ConfigFactory

trait UsingActorSystem {
  def actorSystem[F[_]](name: String)(implicit F: Async[F]): Resource[F, ActorSystem] = for {
    config  <- Resource.liftF(F.delay(ConfigFactory.load()))
    acquire =  F.delay(ActorSystem(name, config))
    release =  (s: ActorSystem) => IO.fromFuture(IO(s.terminate())).to[F].void
    sys     <- Resource.make(acquire)(release)
  } yield sys
}
