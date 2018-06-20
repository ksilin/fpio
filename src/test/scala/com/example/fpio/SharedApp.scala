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

import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref

import scala.concurrent.duration._

object SharedApp extends IOApp {
  // init to null -> BAD!
  def putStrLn(str: String) = IO(println(str))

  // *> is an alias for productR FKA followedBy

  def process1(state: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("proc1") *>
    IO.sleep(15.seconds) *>
    state.update(_ ++ List("1"))
    putStrLn("proc1 done")
  }

  def process2(state: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("proc2") *>
    IO.sleep(10.seconds) *>
    state.update(_ ++ List("2"))
    putStrLn("proc2 done")
  }

  def process3(state: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("proc3") *>
    IO.sleep(5.seconds) *>
    state.update(_ ++ List("3"))
    putStrLn("proc3 done")
  }

  def mainProcess(): IO[Unit] =
    Ref.of[IO, List[String]](Nil) flatMap { state =>
      val procList                = List(process1(state), process2(state), process3(state))
      val procSeq: IO[List[Unit]] = procList.parSequence //
      val i: IO[Unit]             = procSeq.void // from Functor#Ops - but why?
      i *> state.get.flatMap(s => putStrLn(s.mkString))
    }

  override def run(args: List[String]): IO[ExitCode] = mainProcess().as(ExitCode.Success)
}
