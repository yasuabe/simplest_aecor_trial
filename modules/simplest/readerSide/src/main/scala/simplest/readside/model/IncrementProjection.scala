package simplest.readside.model

import aecor.data.Folded

trait IncrementProjection[F[_], E, S] {
  def fetchVersionAndState(event: E): F[(Version, Option[S])]
  def saveNewVersion(s: S, version: Version): F[Unit]
  def applyEvent(s: Option[S])(event: E): Folded[Option[S]]
}
