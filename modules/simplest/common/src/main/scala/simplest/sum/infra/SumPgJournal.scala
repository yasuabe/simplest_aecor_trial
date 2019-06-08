package simplest.sum.infra

import cats.effect.{Async, ContextShift}
import cats.syntax.either._
import doobie.util.transactor.Transactor
import aecor.data._
import aecor.journal.postgres.PostgresEventJournal
import aecor.journal.postgres.PostgresEventJournal.Serializer.TypeHint
import simplest.sum.model.domain.{Added, Created, SumEvent}
import simplest.sum.model.runtime.SumKey

object SumPgJournal {
  val entityName: String = "Sum"
  val tagging: Tagging[SumKey] = Tagging.partitioned(20)(EventTag(entityName))

  def transactor[F[_]: Async: ContextShift]: Transactor[F] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/sumdb",
    "postgres",
    ""
  )
  def eventJournal[F[_]: Async: ContextShift]: SumPgJournal[F] =
    new PostgresEventJournal[F, SumKey, SumEvent](
      transactor,
      "sum_event",
      SumPgJournal.tagging,
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
