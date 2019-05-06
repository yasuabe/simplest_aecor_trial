package simplest

import aecor.MonadActionReject

package object model {
  type IncrementAction[F[_]] = MonadActionReject[F, Option[IncrementState], IncrementEvent, IncrementRejection]
}
