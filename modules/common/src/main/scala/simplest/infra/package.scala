package simplest

import aecor.data.EntityEvent
import aecor.journal.postgres.PostgresEventJournal
import aecor.runtime.Eventsourced.Entities
import simplest.model.{Increment, IncrementEvent, IncrementRejection}

package object infra {
  type Increments[F[_]]       = Entities.Rejectable[IncrementKey, Increment, F, IncrementRejection]
  type IncrementJournal[F[_]] = PostgresEventJournal[F, IncrementKey, IncrementEvent]
  type IncrementEntityEvent   = EntityEvent[IncrementKey, IncrementEvent]
}
