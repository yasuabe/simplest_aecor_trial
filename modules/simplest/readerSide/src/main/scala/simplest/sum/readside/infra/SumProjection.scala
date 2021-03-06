package simplest.sum.readside.infra

import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.effect.Sync
import aecor.data.Folded
import aecor.data.Folded.syntax._
import simplest.sum.model.domain.{Added, Created}
import simplest.sum.model.runtime.SumKey
import simplest.sum.readside.model.{SumView, Version}

import scala.collection.mutable

class SumProjection[F[_]](implicit val F: Sync[F]) {
  private val repo = mutable.Map.empty[SumKey, SumView]

  private def set(view: SumView) = F.delay(repo.update(view.id, view))
  private def get(key: SumKey)   = F.delay(repo.get(key))

  private def raiseIllegalState(state: Any, event: Any): F[Option[SumView]] = {
    val t: Throwable = new IllegalStateException(s"projection failed: state = [$state], event = [$event]")
    t.raiseError[F, Option[SumView]]
  }
  private def applyEvent(v: Option[SumView])(e: SumEntityEvent)
  : Folded[Option[SumView]] = v match {
    case None       => SumView(e.entityKey, 0, Version.zero).some.next
    case Some(view) => e.payload match {
      case Created    => impossible
      case Added(num) => (view add num).some.next
    }
  }
  private def saveNewVersion(s: SumView, v: Version): F[Unit] = set(s version v)

  private def fetchVersionAndState(event: SumEntityEvent): F[(Version, Option[SumView])] =
    get(event.entityKey) map { optView =>
      optView.fold(Version.zero)(_.version) -> optView
    }
  private def foldEvent(event: SumEntityEvent, state: Option[SumView]): F[Option[SumView]] = {
    val newVersion = applyEvent(state)(event)
    val next       = (s: Option[SumView]) => s.pure[F]
    val impossible = raiseIllegalState(state, event)

    newVersion.fold(impossible)(next)
  }
  private def saveIfAny(v: Version)(s: Option[SumView]): F[Unit] = s match {
    case None        => F.unit
    case Some(state) => saveNewVersion(state, v.next)
  }
  private def runProjection(event: SumEntityEvent): F[Unit] =
    for {
      (v0, s0) <- fetchVersionAndState(event)
      _        <- F.whenA(v0 olderThan event)(foldEvent(event, s0) >>= saveIfAny(v0))
      (v1, s1) <- fetchVersionAndState(event)
      _        <- F.delay { println(s"Event: $event, Updated: $v1, $s1") }
    } yield ()

  val sink: fs2.Sink[F, SumCommittable[F]] =
    _.evalMap(_.traverse(runProjection) >>= (_.commit))
}

object SumProjection {
  def sink[F[_]: Sync]: fs2.Sink[F, SumCommittable[F]] =
    new SumProjection[F].sink
}