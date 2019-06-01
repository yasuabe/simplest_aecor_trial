package simplest.increment.readside.model

import aecor.data.EntityEvent

final case class Version(value: Long) extends AnyVal {
  def next: Version = Version(value + 1)
  def olderThan[K, E](e: EntityEvent[K, E]): Boolean = value < e.sequenceNr
}
object Version {
  def apply(value: Option[Long]): Version = Version(value getOrElse 0L)
}