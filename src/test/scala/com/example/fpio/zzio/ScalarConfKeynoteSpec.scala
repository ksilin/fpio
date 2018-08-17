package com.example.fpio.zzio

import com.example.fpio.Timed
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FreeSpec, MustMatchers }
import scalaz.zio._

import scala.util.Random

class ScalarConfKeynoteSpec
    extends FreeSpec
    with MustMatchers
    with ScalaFutures
    with RTS
    with Timed {

  // https://www.youtube.com/watch?v=Eihz7kqn6mU
  // https://www.slideshare.net/jdegoes/scalaz-8-vs-akka-actors

  val encrypt: Int => IO[Nothing, Int] = chunk =>
    IO.sync({
      Thread.sleep(Random.nextInt(10 * chunk))
//      println(s"done with $chunk")
      chunk * 10
    })

  val chunks: List[Int] = (1 to 100).toList

  "must run concurrent computations" in {

    // in example, IO.concurrently is used, no longer available

    timed("parTraverse") {
      val encAll: IO[Nothing, List[Int]]      = IO.parTraverse(chunks)(encrypt(_))
      val res: ExitResult[Nothing, List[Int]] = unsafeRunSync(encAll)
      println(res)
    } // 2200 - 2800 ms

    val description                    = "parTraverse async"
    val start                          = System.nanoTime()
    val encAll: IO[Nothing, List[Int]] = IO.parTraverse(chunks)(encrypt(_))
    // Callback[E, A] = ExitResult[E, A] => Unit
    val res: Unit = unsafeRunAsync(encAll) { r: ExitResult[Nothing, List[Int]] =>
      println(s"timed $description: ${(System.nanoTime() - start) / 1e6} ms")
      println(r)
    } // 2000 - 2700
    Thread.sleep(3000)

//    timed("traverse") {
//      val encAll = IO.traverse(chunks)(encrypt(_))
//      val res    = unsafeRunSync(encAll)
//      println(res)
//    } // 23973 ms
  }

  "ioref concurrent modification" in {

    // effectfully created effectful actor: G[A => F[B]]
    // G - creation effect
    // A - input msg
    // F - output effect
    // B - output msg
    // def x(n: Int): IO[Void, Int] = _
    // def makeActor: IO[Void, Int => IO[Void, Int]]

    val counterio: IO[Nothing, Ref[Int]] = Ref(0)

    val incrementer: Ref[Int] => Int => IO[Nothing, Int] =
      ctr =>
        inc =>
          for {
            res <- ctr.update(_ + inc)
          } yield res

    val incs: IO[Nothing, List[IO[Nothing, Int]]] = for {
      ctr <- counterio
      incs = chunks.map(_ => incrementer(ctr)(1))
    } yield incs

    val r: IO[Nothing, List[Int]] = for {
      inccs <- incs
      res   <- IO.parAll(inccs)
    } yield res

    val res: List[Int] = unsafeRun(r)
    println(res)
  }

  "queue-based solution for high contention scenarios" in {

    type Actor[E, I, O] = I => IO[E, O]

    def persistIn[E, I, O](actor: Actor[E, I, O]): Actor[E, I, O]  = ???
    def persistOut[E, I, O](actor: Actor[E, I, O]): Actor[E, I, O] = ???
    def compose[E, I, O, U](actor: Actor[E, I, O]): Actor[E, O, U] = ???

    val runQueueWorker: (
        Ref[Int],
        Queue[(Int, Promise[Void, Int])]
    ) => IO[Nothing, Fiber[Nothing, Nothing]] = (
        counter: Ref[Int],
        queue: Queue[(Int, Promise[Void, Int])]
    ) =>
      queue.take
        .flatMap(t => counter.update(_ + t._1).flatMap(t._2.complete))
        .forever
        .fork // worker - we dont need the value, just run it

    val defActor: Queue[(Int, Promise[Void, Int])] => Int => IO[Void, Int] = queue =>
      n =>
        for {
          promise <- Promise.make[Void, Int]
          _       <- queue.offer((n, promise))
          value   <- promise.get
        } yield value

    val makeActor: IO[Nothing, Actor[Void, Int, Int]] = {
      for {
        counter <- Ref(0)
        queue   <- Queue.bounded[(Int, Promise[Void, Int])](32)
        _       <- runQueueWorker(counter, queue)
        actor = defActor(queue)
      } yield actor
    }

    val actor: Actor[Void, Int, Int] = unsafeRun(makeActor)
    val x: IO[Void, Int]             = actor(10) flatMap (_ => actor(20))
    val res                          = unsafeRun(x)
    println(res)
    res mustBe 30

  }

}
