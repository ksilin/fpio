package com.example.fpio.iocompare

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.IntegrationPatience
import akka.actor.ActorSystem
import com.example.fpio.iocompare.akkauntyped.AkkaRateLimiter

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

class AkkaRateLimiterSpec
    extends RateLimiterBaseSpec
    with BeforeAndAfterAll
    with IntegrationPatience {

  var system: ActorSystem = _

  override protected def beforeAll(): Unit =
    system = ActorSystem("akka-rate-limiter")

  override protected def afterAll(): Unit =
    system.terminate()

  override def create: Int => FiniteDuration => RateLimiter =
    maxRuns =>
      per =>
        new RateLimiter {
          private val rl = AkkaRateLimiter.create(maxRuns, per)(system)

          override def runLimited(f: => Unit): Unit = rl.runLimited(Future { f })
          override def stop(): Unit                 = rl.stop()

    }
}
