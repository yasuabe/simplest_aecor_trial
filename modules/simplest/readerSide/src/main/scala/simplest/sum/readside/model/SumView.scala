package simplest.sum.readside.model

import simplest.sum.infra.SumKey

case class SumView(id: SumKey, sum: Int, version: Long) {
  def add(num: Int):       SumView = copy(sum = sum + num)
  def version(v: Version): SumView = copy(version = v.value)
}
