package com.example.fpio.iocompare.akkatyped

import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.{ Behaviors, TimerScheduler }
import com.example.fpio.iocompare.RateLimiterQueue
import com.example.fpio.iocompare.RateLimiterQueue.{ Run, RunAfter }
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration._

class AkkaTypedRateLimiter(actorSystem: ActorSystem[RateLimiterMsg]) extends StrictLogging {

  def runLimited[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val p = Promise[T]
    actorSystem ! LazyFuture(() => f.andThen { case r => p.complete(r) }.map(_ => ()))
    p.future
  }

  def stop(): Future[Unit] = {
    import actorSystem.executionContext
    actorSystem.terminate().map { _ =>
      // system logger is no longer available
      logger.info("Stopping rate limiter")
    }
  }

}

object AkkaTypedRateLimiter {

  def create(maxRuns: Int, per: FiniteDuration): AkkaTypedRateLimiter = {
    val behavior = Behaviors.withTimers[RateLimiterMsg] { timer =>
      rateLimit(timer, RateLimiterQueue(maxRuns, per.toMillis))
    }
    new AkkaTypedRateLimiter(ActorSystem(behavior, "rate-limiter"))
  }

  def rateLimit(timer: TimerScheduler[RateLimiterMsg],
                data: RateLimiterQueue[LazyFuture]): Behavior[RateLimiterMsg] =
    Behaviors.receiveMessage {
      case lf: LazyFuture    => rateLimit(timer, runQueue(timer, data.enqueue(lf)))
      case ScheduledRunQueue => rateLimit(timer, runQueue(timer, data.notScheduled))
    }

  def runQueue(timer: TimerScheduler[RateLimiterMsg],
               data: RateLimiterQueue[LazyFuture]): RateLimiterQueue[LazyFuture] = {
    val now            = System.currentTimeMillis()
    val (tasks, data2) = data.run(now)
    tasks.foreach {
      case Run(LazyFuture(f)) => f()
      case RunAfter(millis)   => timer.startSingleTimer((), ScheduledRunQueue, millis.millis)
    }
    data2
  }

}

sealed trait RateLimiterMsg
case class LazyFuture(t: () => Future[Unit]) extends RateLimiterMsg
case object ScheduledRunQueue                extends RateLimiterMsg
