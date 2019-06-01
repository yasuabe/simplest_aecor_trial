package simplest.readside.model

import simplest.infra.IncrementKey

case class IncrementView(id: IncrementKey, sum: Int, version: Long) {
  def add(num: Int):       IncrementView = copy(sum = sum + num)
  def version(v: Version): IncrementView = copy(version = v.value)
}
