package simplest.increment.model

sealed trait IncrementRejection

case object IncrementNotFound$ extends IncrementRejection
case object IncrementAlreadyExists$ extends IncrementRejection
