package simplest.readside.model

import aecor.data.Folded

trait IncrementProjection[F[_], E, S] { // TODO E, S は決め打ちでも良い
  def fetchVersionAndState(event: E): F[(Version, Option[S])]
  def saveNewVersion(s: S, version: Version): F[Unit]
  def applyEvent(s: Option[S])(event: E): Folded[Option[S]]
}
