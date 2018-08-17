package com.example.fpio
import scala.util.Try

package object crawler {

  type Host = String
  case class Url(host: Host, path: String)

  sealed trait CrawlerMessage

  /**
    * Start the crawling process for the given URL. Should be sent only once.
    */
  case class Start(url: Url)                         extends CrawlerMessage
  case class CrawlResult(url: Url, links: List[Url]) extends CrawlerMessage

  sealed trait WorkerMessage
  case class Crawl(url: Url)                              extends WorkerMessage
  case class HttpGetResult(url: Url, result: Try[String]) extends WorkerMessage

}
