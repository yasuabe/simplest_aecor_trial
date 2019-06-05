package simplest.sum.model

sealed trait SumRejection

case object SumNotFound$$$ extends SumRejection
case object SumAlreadyExists$$ extends SumRejection
