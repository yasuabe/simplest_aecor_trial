package simplest.increment.infra

import aecor.data._
import aecor.journal.postgres.PostgresEventJournal
import aecor.journal.postgres.PostgresEventJournal.Serializer.TypeHint
import cats.effect.{Async, ContextShift}
import cats.syntax.either._
import doobie.util.transactor.Transactor
import simplest.increment.model.{IncrementEvent, Incremented, Created}

object PostgresJournal {
  val entityName: String = "Increment"
  val tagging: Tagging[IncrementKey] = Tagging.partitioned(20)(EventTag(entityName))

  def eventJournal[F[_]: Async: ContextShift](transactor: Transactor[F]): IncrementJournal[F] =
    new PostgresEventJournal[F, IncrementKey, IncrementEvent](
      transactor,
      "increment_event",
      PostgresJournal.tagging,
      IncrementEventSerializer
    )
  object IncrementEventSerializer extends PostgresEventJournal.Serializer[IncrementEvent] {
    import simplest.increment.common.protobuf.msg
    def serialize(a: IncrementEvent): (TypeHint, Array[Byte]) = a match {
      case Created          => "A" -> msg.Created().toByteArray
      case Incremented(num) => "B" -> msg.Added(num).toByteArray
    }
    def deserialize(typeHint: TypeHint, bytes: Array[Byte]): Either[Throwable, IncrementEvent] =
      Either.catchNonFatal(typeHint match {
        case "A" => Created
        case "B" => Incremented(msg.Added.parseFrom(bytes).num)
      })
  }
}
