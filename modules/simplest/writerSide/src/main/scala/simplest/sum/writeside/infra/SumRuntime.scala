package simplest.sum.writeside.infra

import aecor.data.{ActionT, EitherK, EventsourcedBehavior}
import aecor.runtime.Eventsourced
import aecor.runtime.akkageneric.{GenericAkkaRuntime, GenericAkkaRuntimeSettings}
import akka.actor.ActorSystem
import cats.Monad
import cats.effect.{ConcurrentEffect, Timer}
import cats.syntax.functor._
import scodec.Codec
import boopickle.Default._
import cats.data.EitherT
import simplest.sum.infra.{SumJournal, Sums}
import simplest.sum.model.{Sum, SumEvent, SumRejection, SumState}
import simplest.sum.infra.PostgresJournal

object SumRuntime {
  implicit val rejectionCodec: Codec[SumRejection] =
    aecor.macros.boopickle.BoopickleCodec.codec[SumRejection]

  def behavior[F[_]: Monad]: EventsourcedBehavior[
    EitherK[Sum, SumRejection, ?[_]], // M[_[_]]
    F,                                // F[_]
    Option[SumState],                 // S
    SumEvent                          // E
  ] = {
    type SumAction[X]      = ActionT[F, Option[SumState], SumEvent, X]
    type SumOrRejection[Y] = EitherT[SumAction, SumRejection, Y]
    EventsourcedBehavior.optionalRejectable(
      actions = Sum[SumOrRejection],
      create  = event          => SumState.init(event),
      update  = (state, event) => state handle event
    )
  }

  def sums[F[_]: ConcurrentEffect: Timer](
    system:  ActorSystem,
    journal: SumJournal[F]
  ): F[Sums[F]] = GenericAkkaRuntime(system).runBehavior(
      typeName       = PostgresJournal.entityName,
      createBehavior = Eventsourced(behavior, journal),
      settings       = GenericAkkaRuntimeSettings.default(system)
    ).map(Eventsourced.Entities.fromEitherK(_))
}
