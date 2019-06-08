package simplest.sum.infra

import cats.effect.{Async, ContextShift}
import doobie.util.transactor.Transactor

trait UsesTransactor[F[_]] {
  def transactor(implicit A: Async[F], C: ContextShift[F]): Transactor[F] =
    Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/sumdb",
    "postgres",
    ""
  )
}
