package simplest.sum

import aecor.MonadActionReject

package object model {
  type SumAction[F[_]] = MonadActionReject[F, Option[SumState], SumEvent, SumRejection]
}
