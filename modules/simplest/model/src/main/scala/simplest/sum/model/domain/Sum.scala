package simplest.sum.model.domain

import aecor.data.Folded.syntax._
import aecor.data.{ActionT, EventsourcedBehavior, Folded}
import aecor.macros.boopickleWireProtocol
import boopickle.Default._
import cats.Monad
import cats.data.EitherT
import cats.syntax.flatMap._
import cats.tagless.autoFunctorK

@autoFunctorK(false)
@boopickleWireProtocol
trait Sum[F[_]] {
  def create: F[Unit]
  def add(d: Int): F[Unit]
}

object Sum {
  // F には EitherT[ActionT[G, Option[SumState], SumEvent, ?], SumRejection, ?]のような
  // 型がくるが、 G が Monad でさえあれば、cats や Aecor が提供する暗黙の型クラスインスタンス群を通じて、
  // MonadActionReject[
  //   EitherT[
  //     ActionT[G, Option[SumState], SumEvent, ?],
  //     SumRejection,
  //     ?],
  //   Option[SumState],
  //   SumEvent,
  //   SumRejection]
  // の暗黙インスタンスが芋づる式に導出される
  def apply[F[_]](implicit F: SumActionReject[F]): Sum[F] = new Sum[F] {
    import F._

    def create:      F[Unit] = read flatMap {
      case Some(_) => reject(SumAlreadyExists)
      case None    => append(Created)
    }
    def add(d: Int): F[Unit] = read flatMap {
      case Some(s) => append(Added(d))
      case None    => reject(SumNotFound)
    }
  }
}

sealed trait SumEvent extends Product with Serializable
case object Created         extends SumEvent
case class  Added(num: Int) extends SumEvent

sealed trait SumRejection
case object SumNotFound      extends SumRejection
case object SumAlreadyExists extends SumRejection

case class SumState(sum: Int) {
  def handle(e: SumEvent): Folded[SumState] = e match {
    case Created    => impossible
    case Added(num) => SumState(sum + num).next
  }
}

object SumState {
  def init(e: SumEvent): Folded[SumState] = e match {
    case Created  => SumState(1).next
    case Added(_) => impossible
  }
}

object sumBehavior {
  def apply[F[_]: Monad]: SumBehavior[F] = {
    type SumAction[A]      = ActionT[F, Option[SumState], SumEvent, A]
    type SumOrRejection[B] = EitherT[SumAction, SumRejection, B]
    EventsourcedBehavior.optionalRejectable(
      actions = Sum[SumOrRejection],
      create  = event          => SumState.init(event),
      update  = (state, event) => state handle event
    )
  }
}