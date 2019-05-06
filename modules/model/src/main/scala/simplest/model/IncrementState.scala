package simplest.model

import aecor.data.Folded
import aecor.data.Folded.syntax._

case class IncrementState(sum: Int) {
  def add(num: Int): IncrementState = IncrementState(sum + num)
  def handle(e: IncrementEvent): Folded[IncrementState] = e match {
    case NumberCreated    => impossible
    case NumberAdded(num) => add(num).next
  }
}

object IncrementState {
  def init(e: IncrementEvent): Folded[IncrementState] = e match {
    case NumberCreated  => IncrementState(1).next
    case NumberAdded(_) => impossible
  }
}
