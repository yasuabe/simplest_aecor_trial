package simplest.sum.infra

import cats.effect.{Async, ContextShift}
import cats.syntax.either._
import doobie.util.transactor.Transactor
import aecor.data._
import aecor.journal.postgres.PostgresEventJournal
import PostgresEventJournal.Serializer
import Serializer.TypeHint
import simplest.sum.model.domain.{Added, Created, SumEvent}
import simplest.sum.model.runtime.SumKey

object SumPgJournal {
  val entityName: String = "Sum"
  val tagging: Tagging[SumKey] = Tagging.partitioned(20)(EventTag(entityName))

  private val serializer = new Serializer[SumEvent] {
    import simplest.sum.common.protobuf.msg
    def serialize(a: SumEvent): (TypeHint, Array[Byte]) = a match {
      case Created    => "C" -> msg.Created().toByteArray
      case Added(num) => "A" -> msg.Added(num).toByteArray
    }
    def deserialize(typeHint: TypeHint, bytes: Array[Byte]): Either[Throwable, SumEvent] =
      Either.catchNonFatal(typeHint match {
        case "C" => Created
        case "A" => Added(msg.Added.parseFrom(bytes).num)
      })
  }
  def journal[F[_]: Async: ContextShift](transactor: Transactor[F]): SumPgJournal[F] =
    new PostgresEventJournal[F, SumKey, SumEvent](
      transactor,
      "sum_event",
      tagging,
      serializer
    )
}
