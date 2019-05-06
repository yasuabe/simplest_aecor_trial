package simplest.infra

import java.util.UUID

case class IncrementKey(value: String) extends AnyVal

object IncrementKey {
  def random(): IncrementKey = IncrementKey(UUID.randomUUID().toString)
}
