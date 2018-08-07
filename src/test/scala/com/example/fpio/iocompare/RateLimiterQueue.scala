package com.example.fpio.iocompare

import scala.collection.immutable.Queue

// https://github.com/softwaremill/akka-vs-scalaz/blob/master/core/src/main/scala/com/softwaremill/ratelimiter/RateLimiterQueue.scala

case class RateLimiterQueue[F](maxRuns: Int, perMillis: Long,
                               lastTimestamps: Queue[Long], waiting: Queue[F], scheduled: Boolean){

  def enqueue(f: F): RateLimiterQueue[F] = copy(waiting = waiting.enqueue(f))

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
