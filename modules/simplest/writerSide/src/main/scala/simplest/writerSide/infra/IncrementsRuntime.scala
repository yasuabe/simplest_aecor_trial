package simplest.writerSide.infra

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
import simplest.model.{Increment, IncrementEvent, IncrementRejection, IncrementState}
import simplest.infra._

object IncrementsRuntime {
  implicit val rejectionCodec: Codec[IncrementRejection] =
    aecor.macros.boopickle.BoopickleCodec.codec[IncrementRejection]

  def behavior[F[_]: Monad]: EventsourcedBehavior[
    EitherK[Increment, IncrementRejection, ?[_]], // M[_[_]]
    F,                                            // F[_]
    Option[IncrementState],                       // S
    IncrementEvent                                // E
  ] = {
    type IncrementAction[X]      = ActionT[F, Option[IncrementState], IncrementEvent, X]
    type IncrementOrRejection[Y] = EitherT[IncrementAction, IncrementRejection, Y]
    EventsourcedBehavior.optionalRejectable(
      actions = Increment[IncrementOrRejection],
      create  = event          => IncrementState.init(event),
      update  = (state, event) => state handle event
    )
  }

  def increments[F[_]: ConcurrentEffect: Timer](
    system:  ActorSystem,
    journal: IncrementJournal[F]
  ): F[Increments[F]] = GenericAkkaRuntime(system).runBehavior(
      typeName       = PostgresJournal.entityName,
      createBehavior = Eventsourced(behavior, journal),
      settings       = GenericAkkaRuntimeSettings.default(system)
    ).map(Eventsourced.Entities.fromEitherK(_))
}
