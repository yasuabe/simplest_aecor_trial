package simplest.sum.readside

import aecor.data.{Committable, EntityEvent}
import simplest.sum.model.domain.SumEvent
import simplest.sum.model.runtime.SumKey

package object infra {
  type SumEntityEvent       = EntityEvent[SumKey, SumEvent]
  type SumCommittable[F[_]] = Committable[F, SumEntityEvent]
}
