package simplest.sum

import aecor.data.{Committable, EntityEvent, TagConsumer}
import aecor.journal.postgres.{Offset, PostgresEventJournal}
import aecor.runtime.Eventsourced.Entities
import aecor.runtime.KeyValueStore
import simplest.sum.model.{Sum, SumEvent, SumRejection}

package object infra {
  type Sums[F[_]]             = Entities.Rejectable[SumKey, Sum, F, SumRejection]
  type SumJournal[F[_]]       = PostgresEventJournal[F, SumKey, SumEvent]
  type SumEntityEvent         = EntityEvent[SumKey, SumEvent]
  type SumCommittable[F[_]]   = Committable[F, SumEntityEvent]
  type SumOffsetStore[F[_]] = KeyValueStore[F, TagConsumer, Offset]
}
