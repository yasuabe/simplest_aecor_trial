package simplest.sum.model

import aecor.data.Folded
import aecor.data.Folded.syntax._

case class SumState(sum: Int) {
  def add(num: Int): SumState = SumState(sum + num)
  def handle(e: SumEvent): Folded[SumState] = e match {
    case Created    => impossible
    case Added(num) => add(num).next
  }
}

object SumState {
  def init(e: SumEvent): Folded[SumState] = e match {
    case Created  => SumState(1).next
    case Added(_) => impossible
  }
}
