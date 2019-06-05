package simplest.sum

import aecor.MonadActionReject

package object model {
  type CounterAction[F[_]] = MonadActionReject[F, Option[SumState], SumEvent, SumRejection]
}