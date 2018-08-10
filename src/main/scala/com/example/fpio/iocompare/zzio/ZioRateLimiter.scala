package com.example.fpio.iocompare.zzio
import com.example.fpio.iocompare.RateLimiterQueue
import com.example.fpio.iocompare.RateLimiterQueue._
import com.typesafe.scalalogging.StrictLogging
import scalaz.zio.{ Fiber, IO, Promise, Queue }
import cats.implicits._
import IOInstances._
import scala.concurrent.duration._

import cats.Monad
import scalaz.zio.IO

class ZioRateLimiter(queue: Queue[RateLimiterMsg], runQueueFiber: Fiber[Nothing, Unit]) {}

object ZioRateLimiter extends StrictLogging {

  def create(maxRuns: Int, per: FiniteDuration): IO[Nothing, ZioRateLimiter] =
    for {
      queue <- Queue.bounded[RateLimiterMsg]
      runQFiber <- runQueue(RateLimiterQueue(maxRuns, per.toMillis), queue)
        .ensuring(IO.sync(logger.info("stopping rate limiter"))) // executes finalizer, whether action succeeds, fails or is interrupted
        .fork

    } yield new ZioRateLimiter(queue, runQFiber)

  private def runQueue(data: RateLimiterQueue[IO[Void, Unit]],
                       queue: Queue[RateLimiterMsg]): IO[Nothing, Unit] = {
    val enqueue: IO[Nothing, RateLimiterQueue[IO[Void, Unit]]] = queue.take.map {
      case ScheduledRunQueue => data.notScheduled
      case Schedule(t)       => data.enqueue(t)
    }

    val runLimiterQueue: IO[Nothing,
                            (List[RateLimiterQueue.RateLimiterTask[IO[Void, Unit]]],
                             RateLimiterQueue[IO[Void, Unit]])] =
      enqueue.map(_.run(System.currentTimeMillis()))

    runLimiterQueue.flatMap {
      case (tasks, q) =>
        tasks
          .map {
            case Run(run) => run
            case RunAfter(millis) =>
              IO.sleep(millis.millis).flatMap(_ => queue.offer(ScheduledRunQueue))
          }
          .map {
            _.fork
          }
          .sequence_
        map(_ => q)
    }

    IO.unit

  }
}

private sealed trait RateLimiterMsg
private case object ScheduledRunQueue             extends RateLimiterMsg
private case class Schedule(t: IO[Nothing, Unit]) extends RateLimiterMsg
