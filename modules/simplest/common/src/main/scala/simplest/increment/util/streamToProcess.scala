package simplest.increment.util

import aecor.distributedprocessing.DistributedProcessing.{Process, RunningProcess}
import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.concurrent.SignallingRef

object streamToProcess {
  def apply[F[_]](stream: fs2.Stream[F, Unit])(implicit F: Concurrent[F]): Process[F] =
    Process(for {
      signal  <- SignallingRef(false)
      running <- F.start(
        stream
          .interruptWhen(signal)
          .compile
          .drain
      )
    } yield RunningProcess(running.join, signal.set(true)))
}
