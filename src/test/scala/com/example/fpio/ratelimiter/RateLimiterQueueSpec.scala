package com.example.fpio.ratelimiter

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FreeSpec, MustMatchers }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RateLimiterQueueSpec extends FreeSpec with MustMatchers with ScalaFutures {

  val q = RateLimiterQueue[() => Future[Int]](2, 1000)

  "must do sth" in {

    val f: () => Future[Int] = () =>
      Future {
        println("executing")
        1
    }

    val res: (List[RateLimiterQueue.RateLimiterTask[() => Future[Int]]],
              RateLimiterQueue[() => Future[Int]]) = q.enqueue(f).run(1001)

    println(res)

  }

}
