package simplest.model

sealed trait IncrementRejection

case object IncrementNotFound$ extends IncrementRejection
case object IncrementAlreadyExists$ extends IncrementRejection
