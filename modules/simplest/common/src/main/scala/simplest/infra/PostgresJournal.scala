package simplest.infra

import aecor.data._
import aecor.journal.postgres.PostgresEventJournal
import aecor.journal.postgres.PostgresEventJournal.Serializer.TypeHint
import cats.effect.{Async, ContextShift}
import cats.syntax.either._
import cats.syntax.functor._
import doobie.util.transactor.Transactor
import simplest.common.protobuf.msg
import simplest.model.{IncrementEvent, NumberAdded, NumberCreated}

class PostgresJournal[F[_]: Async: ContextShift] { // TODO このクラスの意味再考
  import PostgresJournal._

  lazy val transactor: Transactor[F] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/increment",
    "postgres",
    ""
  )
  lazy val journal: IncrementJournal[F] =
    new PostgresEventJournal[F, IncrementKey, IncrementEvent](
      transactor,
      "increment_event",
      PostgresJournal.tagging,
      IncrementEventSerializer
    )
}

object PostgresJournal {
  val entityName: String = "Increment"
  val tagging: Tagging[IncrementKey] = Tagging.partitioned(20)(EventTag(entityName))

  def apply[F[_]: Async: ContextShift]: F[PostgresJournal[F]] = {
    val j = new PostgresJournal[F]
    j.journal.createTable.as(j)
  }
  object IncrementEventSerializer extends PostgresEventJournal.Serializer[IncrementEvent] {
    def serialize(a: IncrementEvent): (TypeHint, Array[Byte]) = a match {
      case NumberCreated    => "A" -> msg.Created().toByteArray
      case NumberAdded(num) => "B" -> msg.Added(num).toByteArray
    }
    def deserialize(typeHint: TypeHint, bytes: Array[Byte]): Either[Throwable, IncrementEvent] =
      Either.catchNonFatal(typeHint match {
        case "A" => NumberCreated
        case "B" => NumberAdded(msg.Added.parseFrom(bytes).num)
      })
  }
}
