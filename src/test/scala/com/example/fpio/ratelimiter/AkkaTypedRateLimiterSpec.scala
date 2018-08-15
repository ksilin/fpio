package com.example.fpio.ratelimiter

import com.example.fpio.ratelimiter.akkatyped.AkkaTypedRateLimiter
import org.scalatest.concurrent.IntegrationPatience

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class AkkaTypedRateLimiterSpec extends RateLimiterBaseSpec with IntegrationPatience {

  override def create: Int => FiniteDuration => RateLimiter =
    maxRuns =>
      per =>
        new RateLimiter {
          private val rl = AkkaTypedRateLimiter.create(maxRuns, per)

          override def runLimited(f: => Unit): Unit = rl.runLimited(Future { f })
          override def stop(): Unit                 = rl.stop()

    }
}
