package com.example.fpio.crawler
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success }

object UsingAkka {
  def crawl(startUrl: Url, http: Http[Future], parseLinks: String => List[Url])(
      implicit system: ActorSystem
  ): Future[Map[Host, Int]] = {
    val res = Promise[Map[Host, Int]]()
    system.actorOf(Props(new AkkaCrawler(http, parseLinks, res))) ! Start(startUrl)
    res.future
  }
}

class AkkaCrawler(http: Http[Future],
                  parseLinks: String => List[Url],
                  result: Promise[Map[Host, Int]])
    extends Actor {

  private var refCount     = Map[Host, Int]() // host popularity
  private var visitedLinks = Set[Url]()
  private var inProgress   = Set[Url]()
  private var workers      = Map[Host, ActorRef]()

  override def receive: Receive = {
    case Start(url) =>
      crawlUrl(url)

    case CrawlResult(url, links) =>
      inProgress -= url
      links.foreach { link =>
        crawlUrl(link)
        refCount = refCount.updated(link.host, refCount.getOrElse(link.host, 0) + 1)
      }
      if (inProgress.isEmpty) {
        result.success(refCount)
        context.stop(self)
      }
  }

  def crawlUrl(url: Url): Unit =
    if (!visitedLinks.contains(url)) {
      println(s"crawling $url")
      visitedLinks += url
      inProgress += url
      actorFor(url.host) ! Crawl(url)
    }

  private def actorFor(host: Host): ActorRef =
    workers.get(host) match {
      case None =>
        val workerActor = context.actorOf(Props(new Worker(http, parseLinks, self)))
        workers += host -> workerActor
        workerActor
      case Some(ar) => ar
    }

}

class Worker(http: Http[Future], parseLinks: String => List[Url], master: ActorRef)
    extends Actor
    with ActorLogging {
  private var urlsPending: Vector[Url] = Vector.empty
  private var getInProgress            = false

  def startHttpGetIfPossible(): Unit =
    urlsPending match {
      case url +: tail if !getInProgress =>
        getInProgress = true
        urlsPending = tail

        import context.dispatcher
        http.get(url).onComplete(r => self ! HttpGetResult(url, r))
      case _ =>
    }

  override def receive: Receive = {
    case Crawl(url) =>
      urlsPending = urlsPending :+ url
      startHttpGetIfPossible()

    case HttpGetResult(url, Success(body)) =>
      getInProgress = false
      startHttpGetIfPossible()
      val links = parseLinks(body)
      master ! CrawlResult(url, links)

    case HttpGetResult(url, Failure(e)) =>
      getInProgress = false
      startHttpGetIfPossible()
      log.error(s"cannot get content of $url", e)
      master ! CrawlResult(url, Nil)

  }
}
