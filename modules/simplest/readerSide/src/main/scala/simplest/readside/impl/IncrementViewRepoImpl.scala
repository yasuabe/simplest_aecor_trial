package simplest.readside.impl

import cats.Monad
import cats.syntax.functor._
import doobie._
import doobie.implicits._
import simplest.infra.IncrementKey
import simplest.readside.model.{IncrementView, IncrementViewRepo}

class IncrementViewRepoImpl[F[_]: Monad](xa: Transactor[F]) extends IncrementViewRepo[F] {

  private val setViewQuery = s"""
    INSERT INTO
      increments (increment_id, sum, version)
    VALUES (?,?,?)
    ON CONFLICT (increment_id)
    DO UPDATE SET
     sum     = EXCLUDED.sum,
     version = EXCLUDED.version;
  """

  private def queryView(key: IncrementKey) =
    (fr"SELECT * FROM increments" ++ fr"WHERE increment_id = $key").query[IncrementView]

  def get(key: IncrementKey): F[Option[IncrementView]] = queryView(key).option.transact(xa)

  def set(view: IncrementView): F[Unit] =
    Update[IncrementView](setViewQuery).run(view).transact(xa).void
}
