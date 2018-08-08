package com.example.fpio.iocompare.monix

// monix Fiber[A] is cats.effect Fiber[Task, A]
import com.example.fpio.iocompare.RateLimiterQueue
import com.example.fpio.iocompare.RateLimiterQueue.{ Run, RunAfter }
import com.typesafe.scalalogging.StrictLogging
import monix.eval.{ Fiber, MVar, Task }
import cats.implicits._

import scala.concurrent.duration._

class MonixRateLimiter(messageQueue: MVar[RateLimiterMsg], queueFiber: Fiber[Unit]) {

  def runLimited[T](f: Task[T]): Task[T] =
    for {
      mv <- MVar.empty[T] // Task[MVar[T]]
      _  <- messageQueue.put(Schedule(f.flatMap(mv.put))) // Task[Unit]
      r  <- mv.take // Task[A]
    } yield r

  def stop(): Task[Unit] = queueFiber.cancel

}

object MonixRateLimiter extends StrictLogging {

  def create(maxRuns: Int, per: FiniteDuration): Task[MonixRateLimiter] =
    for {
      msgQ <- MVar.empty[RateLimiterMsg]
      runQFiber <- runQueue(RateLimiterQueue[Task[Unit]](maxRuns, per.toMillis), msgQ)
        .doOnCancel(Task.eval(logger.info("stopping rate limiter")))
        .fork //executeAsync.start
    } yield new MonixRateLimiter(msgQ, runQFiber)

  // TODO - deconfumbulate
  def runQueue(data: RateLimiterQueue[Task[Unit]], msgQ: MVar[RateLimiterMsg]): Task[Unit] =
    msgQ.take // may block here until msg available
      .map {
        case ScheduledRunQueue => data.notScheduled
        case Schedule(t)       => data.enqueue(t)
      }
      .map(q => q.run(System.currentTimeMillis())) // run queue, obtain tasks
      .flatMap {
        case (tasks, q) =>
          tasks
            .map {
              case Run(run) => run
              case RunAfter(millis) =>
                Task.sleep(millis.millis).flatMap(_ => msgQ.put(ScheduledRunQueue))
            }
            .map { task: Task[Unit] =>
              task.fork // executeAsync.start -> run in background in a fiber - why this one of all exec methods
            }
            // courtesy of cats.syntax.NestedFoldableOps List[Task[Fiber]] -> Task[List[Fiber]]
            .sequence_ // gather tasks into one big task
            .map(_ => q) // ignore results
      }
      .flatMap(q => runQueue(q, msgQ)) // recursively handle the next msg

}

private sealed trait RateLimiterMsg
private case object ScheduledRunQueue      extends RateLimiterMsg
private case class Schedule(t: Task[Unit]) extends RateLimiterMsg
