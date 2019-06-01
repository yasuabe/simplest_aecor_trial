package simplest.increment.readside.infra

import aecor.data.Folded
import aecor.data.Folded.syntax._
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import simplest.increment.infra.{IncrementCommittable, IncrementEntityEvent, IncrementKey}
import simplest.increment.model.{NumberAdded, NumberCreated}
import simplest.increment.readside.model.{IncrementView, Version}

import scala.collection.mutable

class IncrementProjection[F[_]](implicit val F: Sync[F]) {
  private val repo = mutable.Map.empty[IncrementKey, IncrementView]

  private def set(view: IncrementView) = F.delay(repo.update(view.id, view))
  private def get(key: IncrementKey)   = F.delay(repo.get(key))

  private def raiseIllegalState(state: Any, event: Any): F[Option[IncrementView]] = {
    val t: Throwable = new IllegalStateException(s"projection failed: state = [$state], event = [$event]")
    t.raiseError[F, Option[IncrementView]]
  }
  private def applyEvent(v: Option[IncrementView])(e: IncrementEntityEvent)
  : Folded[Option[IncrementView]] = v match {
    case None       => IncrementView(e.entityKey, 0, 0).some.next
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
      val impossible = raiseIllegalState(state, event)

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
        _        <- F.delay { println(s"Event: $event, Updated: $v1, $s1") }
      } yield ()

    stream.evalMap { committable =>
      committable.traverse(runProjection) >>= (_.commit)
    }
  }
}
