package com.example.fpio.crawler

trait CrawlerTestData {

  trait TestDataSet {
    def expectedCounts: Map[Host, Int]
    def parseLinks: String => List[Url]
    def http: Url => String
    def startingUrl: Url
    def name: String
    def shouldTakeMillisMin: Option[Long] = None
    def shouldTakeMillisMax: Option[Long] = None
  }

  val testDataSets = List(BasicTest,
                          SingleDomainChain,
                          MultipleDomainChain,
                          DenseLinks,
                          SingleDomainChainTimed,
                          DenseLinksTimed)

  object BasicTest extends TestDataSet {
    override val name = "basic"

    override val expectedCounts = Map(
      "d1" -> 6,
      "d2" -> 2,
      "d3" -> 2,
      "d4" -> 2
    )

    override val parseLinks: String => List[Url] = (_: String) match {
      case "body11" => List(Url("d1", "p1"), Url("d1", "p2"), Url("d2", "p1"))
      case "body12" => List(Url("d1", "p1"), Url("d1", "p3"), Url("d2", "p1"), Url("d3", "p1"))
      case "body13" => List(Url("d1", "p3"))
      case "body21" => List(Url("d1", "p2"), Url("d3", "p1"), Url("d4", "p1"))
      case "body31" => List(Url("d4", "p1"))
      case "body41" => Nil
    }

    override val http: Url => Host = {
      case Url("d1", "p1") => "body11"
      case Url("d1", "p2") => "body12"
      case Url("d1", "p3") => "body13"
      case Url("d1", "p4") => "body14"
      case Url("d2", "p1") => "body21"
      case Url("d3", "p1") => "body31"
      case Url("d4", "p1") => "body41"
    }

    override val startingUrl = Url("d1", "p1")
  }

  object SingleDomainChain extends TestDataSet {
    val count = 100000

    override val name = "single domain chain"

    override val expectedCounts = Map(
      "d1" -> count
    )

    override val parseLinks: String => List[Url] = { b =>
      val i = b.toInt
      if (i < count) List(Url("d1", (i + 1).toString)) else Nil
    }

    override val http: Url => String = _.path

    override val startingUrl = Url("d1", "0")
  }

  object MultipleDomainChain extends TestDataSet {
    val count = 100000

    override val name = "multiple domain chain"

    override val expectedCounts: Map[Host, Int] = (1 to count).map { i =>
      i.toString -> 1
    }.toMap

    override val parseLinks: String => List[Url] = { b =>
      val i = b.toInt
      if (i < count) List(Url((i + 1).toString, "p")) else Nil
    }

    override val http: Url => Host = _.host

    override val startingUrl = Url("0", "p")
  }

  object DenseLinks extends TestDataSet {
    val count = 10000

    override val name = "dense links"

    override val expectedCounts: Map[Host, Int] = Map("d" -> count * count)

    val links: List[Url] = (1 to count).map(i => Url("d", i.toString)).toList

    override val parseLinks: String => List[Url] = { _ =>
      links
    }

    override val http: Url => Host = _.host

    override val startingUrl = Url("d", "1")
  }

  object SingleDomainChainTimed extends TestDataSet {
    val count = 10

    override val name = "single domain chain (timed)"

    override val expectedCounts = Map(
      "d1" -> count
    )

    override val parseLinks: String => List[Url] = { b =>
      val i = b.toInt
      if (i < count) List(Url("d1", (i + 1).toString)) else Nil
    }

    override val http: Url => String = { url =>
      Thread.sleep(100)
      url.path
    }

    override val startingUrl = Url("d1", "0")

    override def shouldTakeMillisMin: Option[Long] = Some(1000L)
  }

  object DenseLinksTimed extends TestDataSet {
    val count = 10

    override val name = "dense links (timed)"

    override val expectedCounts: Map[Host, Int] = (1 to count).map { i =>
      i.toString -> count
    }.toMap

    val links: List[Url] = (1 to count).map(i => Url(i.toString, "p")).toList

    override val parseLinks: String => List[Url] = { _ =>
      links
    }

    override val http: Url => String = { url =>
      Thread.sleep(100)
      url.host
    }

    override val startingUrl = Url("1", "p")

    override def shouldTakeMillisMax: Option[Long] = Some(500L)
  }

}
