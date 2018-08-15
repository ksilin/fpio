package com.example.fpio.ratelimiter.akkauntyped

import akka.actor.{ ActorRef, ActorSystem, PoisonPill, Props }
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future, Promise }

class AkkaRateLimiter(rateLimiterActor: ActorRef) {

  def runLimited[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    // println(s"run limited $f")
    val p = Promise[T]
    rateLimiterActor ! LazyFuture(() => f.andThen { case r => p.complete(r) }.map(_ => ()))
    p.future
  }

  def stop(): Unit =
    rateLimiterActor ! PoisonPill

}

object AkkaRateLimiter {
  def create(maxRuns: Int, per: FiniteDuration)(implicit system: ActorSystem): AkkaRateLimiter = {
    val actor = system.actorOf(Props(new RateLimiterActor(maxRuns, per)))
    new AkkaRateLimiter(actor)
  }
}
