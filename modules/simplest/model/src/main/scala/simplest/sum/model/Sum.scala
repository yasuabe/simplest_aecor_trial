package simplest.sum.model

import cats.syntax.flatMap._
import cats.tagless.autoFunctorK
import boopickle.Default._
import aecor.macros.boopickleWireProtocol
import aecor.data.Folded
import aecor.data.Folded.syntax._
import aecor.MonadActionReject

@autoFunctorK(false)
@boopickleWireProtocol
trait Sum[F[_]] {
  def create: F[Unit]
  def add(d: Int): F[Unit]
}

object Sum {
  type SumAction[F[_]] = MonadActionReject[F, Option[SumState], SumEvent, SumRejection]

  def apply[F[_]](implicit F: SumAction[F]): Sum[F] = new Sum[F] {
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
