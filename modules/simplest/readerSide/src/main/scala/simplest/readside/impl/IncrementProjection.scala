package simplest.readside.impl

import aecor.data.Folded
import aecor.data.Folded.syntax._
import cats.effect.Sync
import cats.syntax.option._
import simplest.infra.{IncrementCommittable, IncrementEntityEvent, IncrementKey}
import simplest.model.{NumberAdded, NumberCreated}
import simplest.readside.model.{IncrementView, Version}
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.collection.mutable

class IncrementProjection[F[_]](implicit val F: Sync[F]) {
  private val repo = mutable.Map.empty[IncrementKey, IncrementView]

  private def set(view: IncrementView) = F.delay(repo.update(view.numberId, view))
  private def get(key: IncrementKey)   = F.delay(repo.get(key))

  private def puts(s: String) = F.delay { println(s) }
  private def illegalState(state: Any, event: Any): Throwable =
    new IllegalStateException(s"projection failed: state = [$state], event = [$event]")

  private def applyEvent(v: Option[IncrementView])(e: IncrementEntityEvent)
  : Folded[Option[IncrementView]] = v match {
    case None       => IncrementView(e.entityKey, 1, 0).some.next
    case Some(view) => e.payload match {
      case NumberCreated    => impossible
      case NumberAdded(num) => (view add num).some.next
    }
  }
  private def saveNewVersion(s: IncrementView, v: Version): F[Unit] = set(s version v)

  private def fetchVersionAndState(event: IncrementEntityEvent): F[(Version, Option[IncrementView])] =
    get(event.entityKey) map { optView =>
      Version(optView map (_.version)) -> optView
    }
  def sink: fs2.Sink[F, IncrementCommittable[F]] = stream => {
    def foldEvent(event: IncrementEntityEvent, state: Option[IncrementView]): F[Option[IncrementView]] = {
      val newVersion = applyEvent(state)(event)
      val next       = (s: Option[IncrementView]) => s.pure[F]
      val impossible = illegalState(state, event).raiseError[F, Option[IncrementView]]

      newVersion.fold(impossible)(next)
    }
    def saveIfAny(v: Version)(s: Option[IncrementView]): F[Unit] = s match {
      case None        => F.unit
      case Some(state) => saveNewVersion(state, v.next)
    }
    def runProjection(event: IncrementEntityEvent): F[Unit] =
      for {
        (v0, s0) <- fetchVersionAndState(event)
        _        <- F.whenA(v0 olderThan event)(foldEvent(event, s0) >>= saveIfAny(v0))
        (v1, s1) <- fetchVersionAndState(event)
        _        <- puts(s"Event: $event, Updated: $v1, $s1")
      } yield ()

    stream.evalMap { committable =>
      committable.traverse(runProjection) >>= (_.commit)
    }
  }
}
