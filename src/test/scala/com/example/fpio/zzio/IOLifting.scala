package com.example.fpio.zzio
import java.io.{ FileNotFoundException, IOException }
import java.lang.Exception

import org.scalatest.{ FreeSpec, MustMatchers }
import org.scalatest.concurrent.ScalaFutures
import scalaz.zio.{ IO, RTS }
import scalaz.zio.console._
import scalaz.zio.App

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.ExecutionContext.Implicits.global

class IOLifting extends FreeSpec with MustMatchers with ScalaFutures with RTS {

  // there are many ways to lift a computation into an IO

  val impurePrint: String => Unit          = s => println(s"printing $s")
  val impurePrintF: Future[String] => Unit = fs => fs map impurePrint
  val fail: Any => Unit                    = _ => { throw new Exception("boom"); () }

  val pureCalc: Int => Int => Int = i =>
    j => {
      val res = i * j
      println(res)
      res
  }

  "point - lazy eval" in { // never use it for lifting impure code

    val io: IO[Nothing, Unit] = IO.point(impurePrint("hi"))
//    val res: Unit             = unsafeRun(io)
    val io2: IO[Nothing, Int] = IO.point(pureCalc(2)(3))
    val io3: IO[Nothing, Int] = IO.point(fail()) // no exception on ctor
//    val res2                  = unsafeRun(io3)
  }

  "now - strict eval" in { // never use it for lifting impure code

    // see printouts immediately - the IOs are not run
    val io: IO[Nothing, Unit]  = IO.now(impurePrint("hi"))
    val ioF: IO[Nothing, Unit] = IO.now(impurePrintF(Future { Thread.sleep(100); "wake up" }))
    val io2: IO[Nothing, Int]  = IO.now(pureCalc(2)(3))
    Thread.sleep(100)
    val io3: IO[Nothing, Int] = IO.now(fail()) // boom!
  }

  "sync" in { // for effectful impure code
    val io: IO[Nothing, Unit]  = IO.sync(impurePrint("hi"))
    val ioF: IO[Nothing, Unit] = IO.sync(impurePrintF(Future { Thread.sleep(100); "wake up" }))
    val io2: IO[Nothing, Int]  = IO.sync(pureCalc(2)(3))
    Thread.sleep(100)
  }

  "syncThrowable & catchAll" in {

    val toFail: IO[Throwable, Nothing] = IO.syncThrowable(fail())
    // unsafeRun(toFail) -> an error was not handled by a fiber

    val toAttempt: IO[Nothing, Either[Throwable, Nothing]] = toFail.attempt
    val res: Either[Throwable, Nothing]                    = unsafeRun(toAttempt)
    res mustBe a[Left[_, _]]

    // submerge failures with absolve - v.flatMap(fromEither) - not too sure where this might be usable
    val absolved: IO[Throwable, Nothing] = IO.absolve(toFail.attempt)
//    unsafeRun(absolved)

    val x: IO[IOException, Unit] = toFail.catchAll(e => putStr(s"err: $e"))
    unsafeRun(x)

    // TODO - does not compile:
//    missing parameter type for expanded function
//      [error] The argument types of an anonymous function must be fully known. (SLS 8.5)
//    [error] Expected type was: PartialFunction[?,scalaz.zio.IO[?,Unit]]

//      [error]     val y: IO[Throwable, Unit] = toFail.catchSome {
//    val y: IO[Throwable, Unit] = toFail.catchSome {
//      case _ => putStr("some exception")
//      case fnf @ FileNotFoundException => putStr(s"file not found")
//      case e @ Exception               => putStr(s"ex: $e")
//      case IOException(_) => putStr(s"err")
//      case Exception("boom") => putStr(s"err")
//    }
//    unsafeRun(y)

  }

  "syncException" in {}

  "syncCatch" in {}

  "orElse" in {}

  "redeem" in {}

  "forever" in {}

  "retry, retryFor, retryN" in {}

  // from future?
  "from future" in {

    import com.example.fpio.ratelimiter.zzio.FutureInterop._

    val f: () => Future[Int] = () =>
      Future {
        Thread.sleep(100)
        println("working")
        1
    }
    val ec = scala.concurrent.ExecutionContext.Implicits.global

    val ffIO: IO[Throwable, Int] =
      IO.fromFuture(f)(ec)

    val res: Int = unsafeRun(ffIO)
    res mustBe 1

  }

}
