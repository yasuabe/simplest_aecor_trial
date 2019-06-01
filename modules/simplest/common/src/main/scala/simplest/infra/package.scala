package simplest

import aecor.data.{Committable, EntityEvent, TagConsumer}
import aecor.journal.postgres.{Offset, PostgresEventJournal}
import aecor.runtime.Eventsourced.Entities
import aecor.runtime.KeyValueStore
import simplest.model.{Increment, IncrementEvent, IncrementRejection}

package object infra {
  type Increments[F[_]]             = Entities.Rejectable[IncrementKey, Increment, F, IncrementRejection]
  type IncrementJournal[F[_]]       = PostgresEventJournal[F, IncrementKey, IncrementEvent]
  type IncrementEntityEvent         = EntityEvent[IncrementKey, IncrementEvent]
  type IncrementCommittable[F[_]]   = Committable[F, IncrementEntityEvent]
  type IncrementOffsetStore[F[_]] = KeyValueStore[F, TagConsumer, Offset]
}
