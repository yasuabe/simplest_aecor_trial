package simplest.infra

import aecor.data._
import aecor.distributedprocessing.DistributedProcessing.Process
import aecor.journal.postgres.PostgresEventJournal.Serializer.TypeHint
import aecor.journal.postgres.{Offset, PostgresEventJournal, PostgresOffsetStore}
import aecor.runtime.KeyValueStore
import cats.effect.{Async, Concurrent, ContextShift, Timer}
import cats.syntax.either._
import cats.syntax.functor._
import doobie.util.transactor.Transactor
import simplest.common.protobuf.msg
import simplest.model.{IncrementEvent, NumberAdded, NumberCreated}
import simplest.util.streamToProcess

import scala.concurrent.duration._

class PostgresJournal[F[_]: Async: ContextShift] {
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
  lazy val offsetStore: KeyValueStore[F, TagConsumer, Offset] =
    PostgresOffsetStore("consumer_offset").mapK(transactor.trans)

  def createProcesses(
    sink: fs2.Sink[F, Committable[F, IncrementEntityEvent]]
  )(implicit F: Concurrent[F], G: Timer[F]): List[Process[F]] = {

    val queries = journal.queries(100.millis).withOffsetStore(offsetStore)
    def tagStream(tag: EventTag): fs2.Stream[F, Unit] =
      fs2.Stream
        .force(queries.eventsByTag(tag, ConsumerId("ViewProjection")))
        .map(_.map { case (_, event) => event })
        .through(sink)

    PostgresJournal.tagging.tags.map(t => streamToProcess(tagStream(t)))
  }
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
