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

class CatsIoInitTest extends mutable.Specification {

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

  "async IO 3" >> {

    def asyncTask2(param: Any) = Future {
      Thread.sleep(100)
      println("task 2")
      1
    }

    val io: IO[Future[Int]] = IO { asyncTask2("x") }

    // for unsafeRunAsync, YOU need to resolve the Future inside the callback
    def consumer(e: Either[Throwable, Future[Int]]) = {
      println(s"consuming $e")
      val i = e.right.get
      println(s"consumed $i")
    }

    val res = io.unsafeRunAsync(consumer)
    println(res)
    res must_== ()
  }

}
