package simplest.sum

import aecor.distributedprocessing.DistributedProcessing.{Process, RunningProcess}
import aecor.journal.postgres.PostgresEventJournal
import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.concurrent.SignallingRef
import simplest.sum.model.domain.SumEvent
import simplest.sum.model.runtime.SumKey

package object infra {
  type SumPgJournal[F[_]] = PostgresEventJournal[F, SumKey, SumEvent]

  implicit class Fs2StreamOps[F[_]](val stream: fs2.Stream[F, Unit]) {
    def toProcess(implicit F: Concurrent[F]): Process[F] = {
      val runningProcess = for {
        signal  <- SignallingRef(false)
        running <- F.start(         // stream を別スレッドで流す Fiber
          stream
            .interruptWhen(signal)  // signal が true になったら終わり
            .compile.drain
        )
      } yield RunningProcess(
        watchTermination = running.join,    // stream を流している Fiber の終了を待つ F[Unit]
        shutdown         = signal.set(true) // signal をセットして stream を終わらせる F[Unit]
      )
      Process(runningProcess)
    }
  }
}
