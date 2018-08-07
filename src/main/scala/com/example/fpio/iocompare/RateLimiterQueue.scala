package com.example.fpio.iocompare

import com.example.fpio.iocompare.RateLimiterQueue._

import scala.collection.immutable.Queue

// https://github.com/softwaremill/akka-vs-scalaz/blob/master/core/src/main/scala/com/softwaremill/ratelimiter/RateLimiterQueue.scala

case class RateLimiterQueue[F](maxRuns: Int, perMillis: Long,
                               lastTimestamps: Queue[Long], waiting: Queue[F], scheduled: Boolean){

  def run(now: Long): (List[RateLimiterTask[F]], RateLimiterQueue[F]) = pruneTimestamps(now).doRun(now)

  def enqueue(f: F): RateLimiterQueue[F] = copy(waiting = waiting.enqueue(f))

  /**
    * Before invoking a scheduled `run`, clear the scheduled flag.
    * If needed, the next `run` invocation might include a `RunAfter` task.
    */
  def notScheduled: RateLimiterQueue[F] = copy(scheduled = false)

  def doRun(now: Long): (List[RateLimiterTask[F]], RateLimiterQueue[F]) = {
    if(lastTimestamps.size < maxRuns){
      waiting.dequeueOption match {
        case Some((io, remainingQueue)) =>
          val (tasks, next) = copy(lastTimestamps = lastTimestamps.enqueue(now), waiting = remainingQueue).run(now)
          (Run(io) :: tasks, next)
        case None => (Nil, this)
      }
    } else if (!scheduled) {
      val nextAvailableSlot = perMillis - (now - lastTimestamps.head)
      (List(RunAfter(nextAvailableSlot)), this.copy(scheduled = true))
    } else {
      (Nil, this)
    }
  }

  /**
    * Remove timestamps which are outside of the current time window, that is
    * timestamps which are further from `now` than `timeMillis`.
    */
  private def pruneTimestamps(now: Long): RateLimiterQueue[F] = {
    val threshold = now - perMillis
    copy(lastTimestamps = lastTimestamps.filter(_ >= threshold))
  }
}

object RateLimiterQueue {
  def apply[F](maxRuns: Int, perMillis: Long): RateLimiterQueue[F] =
    RateLimiterQueue[F](maxRuns, perMillis, Queue.empty, Queue.empty, scheduled = false)

  sealed trait RateLimiterTask[F]
  case class Run[F](run: F) extends RateLimiterTask[F]
  case class RunAfter[F](millis: Long) extends RateLimiterTask[F]
}
