package simplest.increment.model

import aecor.data.Folded
import aecor.data.Folded.syntax._

case class IncrementState(sum: Int) {
  def add(num: Int): IncrementState = IncrementState(sum + num)
  def handle(e: IncrementEvent): Folded[IncrementState] = e match {
    case Created    => impossible
    case Incremented(num) => add(num).next
  }
}

object IncrementState {
  def init(e: IncrementEvent): Folded[IncrementState] = e match {
    case Created  => IncrementState(1).next
    case Incremented(_) => impossible
  }
}
