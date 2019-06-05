package simplest.sum.infra

import cats.effect.{Async, ContextShift}
import cats.syntax.either._
import doobie.util.transactor.Transactor
import aecor.data._
import aecor.journal.postgres.PostgresEventJournal
import aecor.journal.postgres.PostgresEventJournal.Serializer.TypeHint
import simplest.sum.model.{Added, Created, SumEvent}

object PostgresJournal {
  val entityName: String = "Sum"
  val tagging: Tagging[SumKey] = Tagging.partitioned(20)(EventTag(entityName))

  def eventJournal[F[_]: Async: ContextShift](transactor: Transactor[F]): SumJournal[F] =
    new PostgresEventJournal[F, SumKey, SumEvent](
      transactor,
      "sum_event",
      PostgresJournal.tagging,
      SumEventSerializer
    )
  object SumEventSerializer extends PostgresEventJournal.Serializer[SumEvent] {
    import simplest.sum.common.protobuf.msg
    def serialize(a: SumEvent): (TypeHint, Array[Byte]) = a match {
      case Created    => "A" -> msg.Created().toByteArray
      case Added(num) => "B" -> msg.Added(num).toByteArray
    }
    def deserialize(typeHint: TypeHint, bytes: Array[Byte]): Either[Throwable, SumEvent] =
      Either.catchNonFatal(typeHint match {
        case "A" => Created
        case "B" => Added(msg.Added.parseFrom(bytes).num)
      })
  }
}
