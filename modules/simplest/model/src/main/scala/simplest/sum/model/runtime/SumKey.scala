package simplest.sum.model.runtime

import java.util.UUID

case class SumKey(value: String) extends AnyVal

object SumKey {
  def random(): SumKey = SumKey(UUID.randomUUID().toString)
}
