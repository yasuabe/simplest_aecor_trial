package simplest.sum.util

import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.concurrent.SignallingRef
import aecor.distributedprocessing.DistributedProcessing.{Process, RunningProcess}

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
