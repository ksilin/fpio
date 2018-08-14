package com.example.fpio.iocompare
import com.example.fpio.iocompare.zzio.ZioRateLimiter
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{ FreeSpec, MustMatchers }
import scalaz.zio.RTS
import scalaz.zio.IO

import scala.concurrent.duration.FiniteDuration

class ZioRateLimiterSpec extends RateLimiterBaseSpec with IntegrationPatience {
  override def create: Int => FiniteDuration => RateLimiter =
    maxRuns =>
      per =>
        new RateLimiter with RTS {
          private val rl: ZioRateLimiter = unsafeRun(ZioRateLimiter.create(maxRuns, per))
          override def runLimited(f: => Unit): Unit =
            unsafeRunAsync(rl.runLimited(IO.syncThrowable(f)))(_ => ())
          override def stop(): Unit = unsafeRunSync(rl.stop())
    }

}
