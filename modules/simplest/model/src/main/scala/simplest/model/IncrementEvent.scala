package simplest.model

// Event
sealed trait IncrementEvent extends Product with Serializable
case object NumberCreated extends IncrementEvent
case class NumberAdded(num: Int) extends IncrementEvent

