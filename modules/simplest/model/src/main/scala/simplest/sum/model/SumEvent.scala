package simplest.sum.model

// Event
sealed trait SumEvent extends Product with Serializable
case object Created extends SumEvent
case class Added(num: Int) extends SumEvent

