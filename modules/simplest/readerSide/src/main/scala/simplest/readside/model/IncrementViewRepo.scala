package simplest.readside.model

import simplest.infra.IncrementKey

trait IncrementViewRepo[F[_]] {
  def set(view: IncrementView): F[Unit]
  def get(key: IncrementKey): F[Option[IncrementView]]
}
