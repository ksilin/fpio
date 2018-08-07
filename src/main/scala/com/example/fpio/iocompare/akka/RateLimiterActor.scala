package com.example.fpio.iocompare.akka
import akka.actor.{Actor, ActorLogging}
import com.example.fpio.iocompare.RateLimiterQueue
import com.example.fpio.iocompare.RateLimiterQueue.{Run, RunAfter}

import scala.concurrent.Future
import scala.concurrent.duration._

class RateLimiterActor(maxRuns: Int, per: FiniteDuration) extends Actor with ActorLogging {

  import context.dispatcher

  private var queue = RateLimiterQueue[LazyFuture](maxRuns, per.toMillis)

  override def receive: Receive = {
    case lf: LazyFuture =>
    queue = queue.enqueue(lf)
    runQueue()
  }

  def runQueue(): Unit = {
    val now = System.currentTimeMillis()

    val (tasks, queue2) = queue.run(now)
    queue = queue2
    tasks foreach {
      case Run(LazyFuture(f)) => f()
      case RunAfter(millis) => context.system.scheduler.scheduleOnce(millis.millis, self, ScheduledRunQueue)
    }
  }

  override def postStop(): Unit = log.info("Stopping rate limiter")

}

private case class LazyFuture(t: () => Future[Unit])
private case object ScheduledRunQueue
