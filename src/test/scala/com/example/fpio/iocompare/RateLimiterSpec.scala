package com.example.fpio.iocompare

import java.time.LocalTime
import java.util.concurrent.atomic.AtomicReference

import org.scalatest.concurrent.Eventually
import org.scalatest.{ FreeSpec, MustMatchers }

import scala.concurrent.duration.{ FiniteDuration, _ }

trait RateLimiterSpec extends FreeSpec with MustMatchers with Eventually {

  def create: Int => FiniteDuration => RateLimiter

  "must rate limit futures scheduled upfront" in {
    val rateLimiter = create(2)(1.second)
    val complete    = new AtomicReference(Vector.empty[Int])
    for (i <- 1 to 7) {
//      Thread.sleep(500)
      println(s"${LocalTime.now} trying to run $i")
      rateLimiter.runLimited {
        println(s"${LocalTime.now} Running $i")
        complete.updateAndGet(_ :+ 1)
      }
    }

    eventually {
      complete.get().size mustBe 7
    }
    rateLimiter.stop()
  }

  "must maintain rate limit" in {}

}

trait RateLimiter {
  def runLimited(f: => Unit): Unit
  def stop(): Unit
}
