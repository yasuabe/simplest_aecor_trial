package simplest.sum.writeside.infra

import aecor.runtime.Eventsourced
import aecor.runtime.akkageneric.{GenericAkkaRuntime, GenericAkkaRuntimeSettings}
import akka.actor.ActorSystem
import boopickle.Default._
import cats.effect.{ConcurrentEffect, Timer}
import cats.syntax.functor._
import scodec.Codec
import simplest.sum.infra.{PostgresJournal, SumJournal, Sums}
import simplest.sum.model.{sumBehavior, SumRejection}

object SumRuntime {
  implicit val rejectionCodec: Codec[SumRejection] =
    aecor.macros.boopickle.BoopickleCodec.codec[SumRejection]

  def sums[F[_]: ConcurrentEffect: Timer](
    system:  ActorSystem,
    journal: SumJournal[F]
  ): F[Sums[F]] = GenericAkkaRuntime(system).runBehavior(
      typeName       = PostgresJournal.entityName,
      createBehavior = Eventsourced(sumBehavior[F], journal),
      settings       = GenericAkkaRuntimeSettings.default(system)
    ).map(Eventsourced.Entities.fromEitherK(_))
}
