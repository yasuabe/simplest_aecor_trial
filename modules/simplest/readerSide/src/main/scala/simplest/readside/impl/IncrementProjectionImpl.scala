package simplest.readside.impl

import aecor.data.Folded
import aecor.data.Folded.syntax._
import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.option._
import simplest.infra.{IncrementEntityEvent, IncrementKey}
import simplest.model.{NumberAdded, NumberCreated}
import simplest.readside.model.{IncrementProjection, IncrementView, Version}

import scala.collection.mutable

class IncrementProjectionImpl[F[_]](implicit val F: Sync[F])
  extends IncrementProjection[F, IncrementEntityEvent, IncrementView] {

  val repo: mutable.Map[IncrementKey, IncrementView] = mutable.Map.empty

  def set(view: IncrementView): F[Unit]                  = F.delay(repo.update(view.numberId, view))
  def get(key: IncrementKey):   F[Option[IncrementView]] = F.delay(repo.get(key))

  def applyEvent(v: Option[IncrementView])(e: IncrementEntityEvent)
  : Folded[Option[IncrementView]] = v match {
    case None       => IncrementView(e.entityKey, 1, 0).some.next
    case Some(view) => e.payload match {
      case NumberCreated    => impossible
      case NumberAdded(num) => (view add num).some.next
    }
  }
  def saveNewVersion(s: IncrementView, v: Version): F[Unit] =
    set(s version v)

  def fetchVersionAndState(event: IncrementEntityEvent): F[(Version, Option[IncrementView])] =
    get(event.entityKey) map { optView =>
      Version(optView map (_.version)) -> optView
    }
}
