package com.example.fpio.iocompare

import com.example.fpio.iocompare.mnx.MonixRateLimiter
import monix.execution.Scheduler.Implicits.global
import monix.eval.{ Fiber, MVar, Task }
import org.scalatest.concurrent.IntegrationPatience

import scala.concurrent.duration._

class MonixRateLimiterSpec extends RateLimiterBaseSpec with IntegrationPatience {

  override def create =
    maxRuns =>
      per =>
        new RateLimiter {
          private val rl                            = MonixRateLimiter.create(maxRuns, per).runSyncUnsafe(Duration.Inf)
          override def runLimited(f: => Unit): Unit = rl.runLimited(Task { f }).runAsync
          override def stop(): Unit                 = rl.stop().runSyncUnsafe(Duration.Inf)
    }
}
