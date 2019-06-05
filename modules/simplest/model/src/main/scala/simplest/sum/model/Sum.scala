package simplest.sum.model

import cats.syntax.flatMap._
import cats.tagless.autoFunctorK
import boopickle.Default._
import aecor.macros.boopickleWireProtocol

@autoFunctorK(false)
@boopickleWireProtocol
trait Sum[F[_]] {
  def create: F[Unit]
  def add(d: Int): F[Unit]
}

object Sum {
  def apply[F[_]](implicit F: CounterAction[F]): Sum[F] =
    new Sum[F] {
    import F._

    def create:      F[Unit] = read flatMap {
      case Some(_) => reject(SumAlreadyExists$$)
      case None    => append(Created)
    }
    def add(d: Int): F[Unit] = read flatMap {
      case Some(s) => append(Added(d))
      case None    => reject(SumNotFound$$$)
    }
  }
}
