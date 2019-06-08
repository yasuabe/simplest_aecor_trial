package simplest.sum

import aecor.MonadActionReject
import aecor.data.{EitherK, EventsourcedBehavior}

package object model {
  type SumAction[F[_]] = MonadActionReject[
    F,                // F[_]
    Option[SumState], // S
    SumEvent,         // E
    SumRejection      // R
  ]

  type SumBehavior[F[_]] = EventsourcedBehavior[
    EitherK[Sum, SumRejection, ?[_]], // M[_[_]]
    F,                                // F[_]
    Option[SumState],                 // S
    SumEvent                          // E
  ]
}
