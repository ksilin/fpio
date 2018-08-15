package com.example.fpio.ratelimiter.akkauntyped

import akka.actor.{ Actor, ActorLogging }
import com.example.fpio.ratelimiter.RateLimiterQueue
import com.example.fpio.ratelimiter.RateLimiterQueue.{ Run, RunAfter }

import scala.concurrent.Future
import scala.concurrent.duration._

class RateLimiterActor(maxRuns: Int, per: FiniteDuration) extends Actor with ActorLogging {

  import context.dispatcher

  private var queue = RateLimiterQueue[LazyFuture](maxRuns, per.toMillis)

  override def receive: Receive = {
    case lf: LazyFuture =>
      println("received new task")
      queue = queue.enqueue(lf)
      runQueue()
    case ScheduledRunQueue =>
      println("received ScheduledRunQueue")
      queue = queue.notScheduled
      runQueue()
  }

  def runQueue(): Unit = {
    val now = System.currentTimeMillis()

    val (tasks, queue2) = queue.run(now)
    println(s"running queue: ${queue2.waiting.size}")
    println(s"tasks: ${tasks.size}")
    queue = queue2
    tasks foreach {
      case Run(LazyFuture(f)) =>
        println("running lazy future")
        f()
      case RunAfter(millis) =>
        println("running offset")
        context.system.scheduler.scheduleOnce(millis.millis, self, ScheduledRunQueue)
      case x => println(s"unexpected $x")
    }
  }

  override def postStop(): Unit = log.info("Stopping rate limiter")

}

private case class LazyFuture(t: () => Future[Unit])
private case object ScheduledRunQueue
