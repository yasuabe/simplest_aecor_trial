package simplest.increment.model

// Event
sealed trait IncrementEvent extends Product with Serializable
case object Created extends IncrementEvent
case class Incremented(num: Int) extends IncrementEvent

