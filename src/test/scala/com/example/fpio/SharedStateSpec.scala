package com.example.fpio

import org.specs2._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.syntax.all._ // provides Apply#Ops#*> - see https://www.scala-exercises.org/cats/apply


import scala.concurrent.duration._

class SharedStateSpec extends mutable.Specification {

  // https://typelevel.org/blog/2018/06/07/shared-state-in-fp.html

  // our state will be represented by a Ref

  object sharedApp1 extends IOApp {

    val myState: Ref[IO, List[String]] = _

    def putStrLn(str: String) = IO(println(str))

    val process1: IO[Unit] = {
      putStrLn("hi") *>
      IO.sleep(5.seconds)
    }

    override def run(args: List[String]): IO[ExitCode] = _
  }

  "using mutable state 1" >> {

  }



}
