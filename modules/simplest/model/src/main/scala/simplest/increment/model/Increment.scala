package simplest.increment.model

import cats.syntax.flatMap._
import cats.tagless.autoFunctorK
import boopickle.Default._
import aecor.macros.boopickleWireProtocol

@autoFunctorK(false)
@boopickleWireProtocol
trait Increment[F[_]] {
  def create: F[Unit]
  def add(d: Int): F[Unit]
}

object Increment {
  def apply[F[_]](implicit F: IncrementAction[F]): Increment[F] =
    new Increment[F] {
    import F._

    def create:      F[Unit] = read flatMap {
      case Some(_) => reject(IncrementAlreadyExists$)
      case None    => append(Created)
    }
    def add(d: Int): F[Unit] = read flatMap {
      case Some(s) => append(Incremented(d))
      case None    => reject(IncrementNotFound$)
    }
  }
}
