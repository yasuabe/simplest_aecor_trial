package simplest.sum.readside

import aecor.data.{Committable, EntityEvent}
import simplest.sum.infra.SumKey
import simplest.sum.model.SumEvent

package object infra {
  type SumEntityEvent       = EntityEvent[SumKey, SumEvent]
  type SumCommittable[F[_]] = Committable[F, SumEntityEvent]
}
