package simplest.readside.impl

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.flatMap._
import aecor.data.{Committable, EntityEvent}
import simplest.readside.model.{IncrementProjection, Version}
import cats.syntax.applicativeError._

object projectionSink {
  private def puts[F[_]](s: String)(implicit F: Sync[F]) = F.delay { println(s) }
  private def illegalState(state: Any, event: Any): Throwable =
    new IllegalStateException(s"projection failed: state = [$state], event = [$event]")

  def apply[F[_], K, E, S](
    projection: IncrementProjection[F, EntityEvent[K, E], S]
  )(implicit F: Sync[F]): fs2.Sink[F, Committable[F, EntityEvent[K, E]]] = stream => {

    def foldEvent(event: EntityEvent[K, E], state: Option[S]): F[Option[S]] = {
      val newVersion = projection.applyEvent(state)(event)
      val next       = (s: Option[S]) => s.pure[F]
      def impossible = illegalState(state, event).raiseError[F, Option[S]]

      puts(s"New version [$newVersion]") >> newVersion.fold(impossible)(next)
    }
    def saveIfAny(v: Version)(s: Option[S]) = s match {
      case None        => F.unit
      case Some(state) => projection.saveNewVersion(state, v.next)
    }
    def runProjection(event: EntityEvent[K, E]): F[Unit] =
      for {
        (v, s) <- projection.fetchVersionAndState(event)
        _      <- puts(s"Current $v [$s]")
        _      <- F.whenA(v olderThan event)(foldEvent(event, s) >>= saveIfAny(v))
      } yield ()

    stream.evalMap { committable =>
      committable.traverse(runProjection) >>= (_.commit)
    }
  }
}
