package com.example.fpio.iocompare.mnx

// monix Fiber[A] is cats.effect Fiber[Task, A]
import com.example.fpio.iocompare.RateLimiterQueue
import com.example.fpio.iocompare.RateLimiterQueue.{ RateLimiterTask, Run, RunAfter }
import com.typesafe.scalalogging.StrictLogging
import monix.eval.{ Fiber, MVar, Task }
import cats.implicits._

import scala.concurrent.duration._

class MonixRateLimiter(messageQueue: MVar[RateLimiterMsg], queueFiber: Fiber[Unit]) {

  def runLimited[T](f: Task[T]): Task[T] = {
    println(s"received task: $f")
    val r = for {
      mv <- MVar.empty[T] // Task[MVar[T]]
      _  <- messageQueue.put(Schedule(f.flatMap(mv.put))) // Task[Unit]
      r  <- mv.take // Task[A]

    } yield r
    r
  }

  def stop(): Task[Unit] = queueFiber.cancel

}

object MonixRateLimiter extends StrictLogging {

  def create(maxRuns: Int, per: FiniteDuration): Task[MonixRateLimiter] =
    for {
      msgQ <- MVar.empty[RateLimiterMsg]
      runQFiber <- runQueue(RateLimiterQueue[Task[Unit]](maxRuns, per.toMillis), msgQ)
        .doOnCancel(Task.eval(logger.info("stopping rate limiter")))
        .fork //executeAsync.start - this is the fiber passed to the queue
    } yield new MonixRateLimiter(msgQ, runQFiber)

  def runQueue(data: RateLimiterQueue[Task[Unit]], msgQ: MVar[RateLimiterMsg]): Task[Unit] = {
    println("taking task out of msg queue")
    val msgTask: Task[RateLimiterMsg] = msgQ.take // may block here until msg available
    println(s"took task out of msg queue: $msgTask")
    val applyMsgToQueue: Task[RateLimiterQueue[Task[Unit]]] = msgTask
      .map {
        case ScheduledRunQueue =>
          println("scheduleRunQueue")
          data.notScheduled
        case Schedule(t) =>
          println(s"schedule $t")
          data.enqueue(t)
      }
    val runQ: Task[(List[RateLimiterTask[Task[Unit]]], RateLimiterQueue[Task[Unit]])] =
      applyMsgToQueue
        .map { q =>
          println("run queue")
          q.run(System.currentTimeMillis())
        } // run queue, obtain tasks

    val runn: Task[RateLimiterQueue[Task[Unit]]] = runQ
      .flatMap {
        case (tasks, q) =>
          println("run tasks")
          runTasks(tasks, q, msgQ)
      }
    runn.flatMap(q => runQueue(q, msgQ)) // recursively handle the next msg
  }

  def runTasks(tasks: List[RateLimiterTask[Task[Unit]]],
               queue: RateLimiterQueue[Task[Unit]],
               msgQ: MVar[RateLimiterMsg]): Task[RateLimiterQueue[Task[Unit]]] = {
    val prepRun: List[Task[Unit]] = tasks
      .map {
        case Run(run) => run
        case RunAfter(millis) =>
          Task.sleep(millis.millis).flatMap(_ => msgQ.put(ScheduledRunQueue))
      }
    val runAndIgnore: Task[RateLimiterQueue[Task[Unit]]] = prepRun
      .map { task: Task[Unit] =>
        task.fork // executeAsync.start -> run in background in a fiber - why this one of all exec methods
      }
      // courtesy of cats.syntax.NestedFoldableOps List[Task[Fiber]] -> Task[List[Fiber]]
      .sequence_ // gather tasks into one big task
      .map(_ => queue) // ignore results
    runAndIgnore
  }

}

private sealed trait RateLimiterMsg
private case object ScheduledRunQueue      extends RateLimiterMsg
private case class Schedule(t: Task[Unit]) extends RateLimiterMsg
