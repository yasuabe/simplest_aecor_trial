package simplest.readside.impl

import aecor.data.Folded
import aecor.data.Folded.syntax._
import cats.Functor
import cats.syntax.functor._
import cats.syntax.option._
import simplest.infra.IncrementEntityEvent
import simplest.model.{NumberAdded, NumberCreated}
import simplest.readside.model.{IncrementProjection, IncrementView, IncrementViewRepo, Version}

class IncrementProjectionImpl[F[_]: Functor](repo: IncrementViewRepo[F])
  extends IncrementProjection[F, IncrementEntityEvent, IncrementView] {

  def applyEvent(v: Option[IncrementView])(e: IncrementEntityEvent)
  : Folded[Option[IncrementView]] = v match {
    case None       => IncrementView(e.entityKey, 1, 0).some.next
    case Some(view) => e.payload match {
      case NumberCreated    => impossible
      case NumberAdded(num) => (view add num).some.next
    }
  }
  def saveNewVersion(s: IncrementView, v: Version): F[Unit] =
    repo.set(s version v)

  def fetchVersionAndState(event: IncrementEntityEvent): F[(Version, Option[IncrementView])] =
    repo.get(event.entityKey) map { optView =>
      Version(optView map (_.version)) -> optView
    }
}
