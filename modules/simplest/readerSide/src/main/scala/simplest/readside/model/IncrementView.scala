package simplest.readside.model

import simplest.infra.IncrementKey

case class IncrementView(numberId: IncrementKey, sum: Int, version: Long) { // TODO name numberID
  def add(num: Int): IncrementView       = copy(sum = sum + num)
  def version(v: Version): IncrementView = copy(version = v.value)
}
