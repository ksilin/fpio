package com.example.fpio.ratelimiter

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FreeSpec, MustMatchers }
import scalaz.zio.{ ExitResult, Fiber, IO, RTS, Promise => ZioPromise }

import scala.annotation.tailrec
import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global

case class PromiseTester[T](p: Promise[T]) {

  @tailrec
  final def run(): String =
    if (p.isCompleted) {
      println("completed")
      "yay"
    } else {
      println("sleeping")
      Thread.sleep(10)
      run()
    }
}

class BlockingCommSpec
    extends FreeSpec
    with MustMatchers
//    with IntegrationPatience
    with ScalaFutures
    with RTS {

  "blocking with Promise" in {

    // no real blocking takes place, the promise just does not complete
    val p: Promise[Int] = Promise[Int]()

    val makeFuture = () => Future.successful(1)

    val tester = PromiseTester(p)

    val io: IO[Nothing, String]                    = IO.sync(tester.run())
    val fiber: IO[Nothing, Fiber[Nothing, String]] = io.fork
    unsafeRunAsync(fiber) { n: ExitResult[Nothing, Fiber[Nothing, String]] =>
      println(s"result: $n")
      ()
    }

    Thread.sleep(100)

    p.tryCompleteWith(makeFuture())

    p.isCompleted mustBe true

  }

  "blocking with zio.Promise" in {}

}
