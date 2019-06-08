package simplest.sum.model

import aecor.runtime.EventJournal
import aecor.runtime.Eventsourced.Entities
import simplest.sum.model.domain.{Sum, SumEvent, SumRejection}

package object runtime {
  type Sums[F[_]]       = Entities.Rejectable[SumKey, Sum, F, SumRejection]
  type SumJournal[F[_]] = EventJournal[F, SumKey, SumEvent]
}
