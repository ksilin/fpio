package com.example.fpio.iocompare
import scala.concurrent.duration.FiniteDuration

class MonixRateLimiterSpec extends RateLimiterBaseSpec {

  override def create: Int => FiniteDuration => RateLimiter =
    ???
}
