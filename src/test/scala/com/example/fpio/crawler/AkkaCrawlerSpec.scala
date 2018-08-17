package com.example.fpio.crawler
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.example.fpio.Timed
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AkkaCrawlerSpec
    extends TestKit(ActorSystem("crawler-test"))
    with FreeSpecLike
    with MustMatchers
    with BeforeAndAfterAll
    with CrawlerTestData
    with ScalaFutures
    with IntegrationPatience
    with Timed {

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  for (testData <- testDataSets) {
    s"must crawl test data set ${testData.name}" in {
      import testData._

      timed(testData.name) {
        UsingAkka
          .crawl(startingUrl, url => Future(http(url)), parseLinks)
          .futureValue mustBe expectedCounts
      }

    }
  }

}
