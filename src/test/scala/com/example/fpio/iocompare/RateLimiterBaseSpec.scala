package com.example.fpio.iocompare

import java.time.LocalTime
import java.util.concurrent.atomic.AtomicReference

import org.scalatest.concurrent.Eventually
import org.scalatest.{ FreeSpec, MustMatchers }

import scala.concurrent.duration.{ FiniteDuration, _ }

trait RateLimiterBaseSpec extends FreeSpec with MustMatchers with Eventually {

  def create: Int => FiniteDuration => RateLimiter

  "must rate limit futures scheduled upfront" in {
    val rateLimiter = create(2)(1.second)
    val complete    = new AtomicReference(Vector.empty[Int])
    for (i <- 1 to 7) {
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

  "must maintain rate limit (approximately)" in {

    val rateLimiter = create(10)(1.second)

    val complete = new AtomicReference(Vector.empty[Long])
    for (i <- 1 to 20) {
      rateLimiter.runLimited {
        println(s"${LocalTime.now} running $i")
        complete.updateAndGet(_ :+ System.currentTimeMillis())
      }
      Thread.sleep(100)
    }

    eventually {
      complete.get().size mustBe 20
    }

    val secondHalf = complete.get().slice(10, 20)
    secondHalf.zip(secondHalf.tail).map { case (p, n) => n - p } foreach { d =>
      (d < 150) mustBe true
      (d > 50) mustBe true
    }
    rateLimiter.stop()
  }

  // TODO - will later invocations be preferred over already queued/scheduled tasks?

}

trait RateLimiter {
  def runLimited(f: => Unit): Unit
  def stop(): Unit
}
