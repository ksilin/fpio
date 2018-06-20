/*
 * Copyright 2018 ksilin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.fpio

import org.specs2._
import cats.effect._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CatsIoInitSpec extends mutable.Specification {

  // https://typelevel.org/blog/2017/05/02/io-monad-for-cats.html

  "cats IO init spec" >> {

    val printLater = IO { println("hi") }
//    printLater must haveClass[IO[Unit]] - does nto work bc actual class is IO#Delay
    printLater must haveSuperclass[IO[Unit]]
    val res = printLater.unsafeRunSync()
    res must_== ()
  }

  "async IO" >> {

    def asyncTask1(param: Any) = Future.successful {
      println("task 1")
      1
    }

    val ioF: IO[Future[Int]] = IO {
      asyncTask1("x")
    }

    println("created IO[Future[Int]]")

    val io1: IO[Int] = IO.fromFuture(ioF)

    println("resolved to IO[Int]")

    val res = io1.unsafeRunSync()
    // "task 1" will be printed after "resolved..."
    res must_== 1
  }

  "sync to async IO 2" >> {

    def consumer(e: Either[Throwable, Int]) =
      IO { println(s"consumed $e") }

    val asyncRes: IO[Unit] = IO { 1 }.runAsync(consumer)
    println(asyncRes)

    asyncRes.unsafeRunSync() must_== 1
  }

  "async IO - unsafeRunAsync" >> {

    def asyncTask(param: Any) = Future {
      Thread.sleep(100)
      println("task 2")
      1
    }

    val io: IO[Future[Int]] = IO { asyncTask("x") }

    // for unsafeRunAsync, YOU need to resolve the Future, or other async call inside the callback
    val callback: Either[Throwable, Future[Int]] => Unit = e => {
      println(s"consuming $e")
      val i = e.right.get
      println(s"consumed $i")
    }

    val res = io.unsafeRunAsync(callback)
    println(res)
    res must_== ()
  }

  "async IO - runasync - the safe version" in {
    def asyncTask(param: Any) = Future {
      Thread.sleep(100)
      println("task 2")
      1
    }

    val io: IO[Future[Int]] = IO { asyncTask("x") }

    val callback: Either[Throwable, Future[Int]] => IO[Unit] = e => {
      IO {
        println(s"consuming $e")
        val i = e.right.get
        println(s"consumed $i")
      }
    }

    val resIo = io.runAsync(callback)
    println(resIo)
    val res = resIo.unsafeRunSync()
    res must_== ()

  }

  "no error handling" >> {

    val task = IO { throw new Exception("boom"); () }
    println(s"task: $task")
    task.unsafeRunSync() must throwAn[Exception].like { case e => e.getMessage === "boom" }
//    task.unsafeRunSync() must throwAn[Exception](message = "boom")
  }

  "error handling - MonadError style" >> {
    val task = IO { throw new Exception("boom"); () }

    val eitherIO: IO[Either[Throwable, Unit]] = task.attempt

    val res: Either[Throwable, Unit] = eitherIO.unsafeRunSync()
    res must haveSuperclass[Either[Throwable, Unit]]
    res must beLeft.like { case e => e.getMessage === "boom" }
  }

//  "using the Effect type" >> {

  // TODO - do I ever need to use the Effect type directly?
  // IIUC - this is the instance you need for IO to work, but I dont quite get how
  // any Effect must define the ability to evaluate as a side-effect

  // Cannot find implicit value for Effect[scala.concurrent.Future]
  // Cannot find implicit value for Effect[List]
  // val task: IO[String] = Effect.toIOFromRunAsync(List("hi", "world"))
  // println(task)
  // val res = task.unsafeRunSync()
  // println(res)
//  }
}
