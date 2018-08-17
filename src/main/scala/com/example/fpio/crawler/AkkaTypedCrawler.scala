package com.example.fpio.crawler
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

class AkkaTypedCrawler(http: Http[Future],
                       parseLinks: String => List[Url],
                       reportTo: ActorRef[Map[Host, Int]]) {

  def crawlerBehavior: Behavior[CrawlerMessage] = Behaviors.setup[CrawlerMessage] { ctx =>
    def receive(data: CrawlerData): Behavior[CrawlerMessage] = Behaviors.receiveMessage {
      case Start(startUrl) => receive(crawlUrl(data, startUrl))
      case CrawlResult(url, links) =>
        val data2 = data.copy(inProgress = data.inProgress - url)
        val data3 = links.foldLeft(data2) {
          case (d, link) =>
            val d2 = d.copy(
              refCount = d.refCount.updated(link.host, d.refCount.getOrElse(link.host, 0) + 1)
            )
            crawlUrl(d2, link)
        }

        if (data3.inProgress.isEmpty) {
          reportTo ! data3.refCount
          Behavior.stopped
        } else {
          receive(data3)
        }
    }

    def crawlUrl(data: CrawlerData, url: Url): CrawlerData =
      if (!data.visitedLinks.contains(url)) {
        val (data2, worker) = workerFor(data, url.host)
        worker ! Crawl(url)
        data2.copy(visitedLinks = data.visitedLinks + url, inProgress = data.inProgress + url)
      } else data

    def workerFor(data: CrawlerData, host: Host): (CrawlerData, ActorRef[WorkerMessage]) =
      data.workers.get(host) match {
        case None =>
          val workerActor = ctx.spawn(workerBehavior(ctx.self), s"worker-$host")
          (data.copy(workers = data.workers + (host -> workerActor)), workerActor)
        case Some(ar) => (data, ar)
      }

    receive(CrawlerData(Map(), Set(), Set(), Map()))
  }

  def workerBehavior(master: ActorRef[CrawlResult]): Behavior[WorkerMessage] =
    Behaviors.setup[WorkerMessage] { ctx =>
      def receive(urlsPending: Vector[Url], getInProgress: Boolean): Behavior[WorkerMessage] =
        Behaviors.receiveMessage {
          case Crawl(url) => startHttpGetIfPossible(urlsPending :+ url, getInProgress)

          case HttpGetResult(url, Success(body)) =>
            val links = parseLinks(body)
            master ! CrawlResult(url, links)
            startHttpGetIfPossible(urlsPending, false)

          case HttpGetResult(url, Failure(e)) =>
            ctx.log.error(s"cannot get contents of $url", e)
            master ! CrawlResult(url, Nil)
            startHttpGetIfPossible(urlsPending, false)
        }

      def startHttpGetIfPossible(urlsPending: Vector[Url],
                                 getInProgress: Boolean): Behavior[WorkerMessage] =
        urlsPending match {
          case url +: tail if !getInProgress =>
            import ctx.executionContext
            http.get(url).onComplete(r => ctx.self ! HttpGetResult(url, r))
            receive(tail, true)
          case _ => receive(urlsPending, getInProgress)
        }

      receive(Vector.empty, false)
    }

}

case class CrawlerData(refCount: Map[Host, Int],
                       visitedLinks: Set[Url],
                       inProgress: Set[Url],
                       workers: Map[Host, ActorRef[WorkerMessage]])
