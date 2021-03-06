package simplest.sum.readside.model

import simplest.sum.model.runtime.SumKey

case class SumView(id: SumKey, sum: Int, version: Version) {
  def add(num: Int):       SumView = copy(sum = sum + num)
  def version(v: Version): SumView = copy(version = v)
}
