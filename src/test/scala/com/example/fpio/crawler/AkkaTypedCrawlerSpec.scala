package com.example.fpio.crawler

import akka.actor.testkit.typed.scaladsl.{ ActorTestKit, TestProbe }
import com.example.fpio.Timed
import org.scalatest._
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class AkkaTypedCrawlerSpec
    extends ActorTestKit
    with FreeSpecLike
    with MustMatchers
    with BeforeAndAfterAll
    with CrawlerTestData
    with ScalaFutures
    with IntegrationPatience
    with Timed {

  override def afterAll: Unit = shutdownTestKit()

  for (testData <- testDataSets) {
    s"must crawl test data set ${testData.name}" in {
      import testData._

      timed(testData.name) {

        val probe = TestProbe[Map[String, Int]]()

        val crawler = spawn(
          new AkkaTypedCrawler(url => Future(http(url)), parseLinks, probe.ref).crawlerBehavior
        )

        crawler ! Start(startingUrl)

        probe.expectMessage(1.minute, expectedCounts)
      }

    }
  }

}
