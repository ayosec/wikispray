package com.ayosec.wikispray

import org.scalatest._
import cc.spray.json._
import org.joda.time.DateTime

import com.ayosec.wikispray.moon.MoonDB

class PagesJsonSpec extends WordSpec with MustMatchers {

  implicit val moon = MoonDB("mongodb://localhost/wikispray-PagesServiceSpec")

  import web.Page
  import web.PageJsonProtocol._

  "A page" must {
    "be dumped in JSON" in {
      val page = Page("first", "second", new DateTime(2000, 12, 31, 23, 59, 59))
      page.toJson.toString must equal ("""{"summary":"first","content":"second","date":"2000-12-31T23:59:59.000Z"}""")
    }

    /*
    "be loaded from JSON" in {
      val page = """{"summary": "foo", "content": "bar", "date": "2000-01-01T00:00:00.000Z"}""".asJson.convertTo[Page]
      page.summary.get must equal ("foo")
      page.content.get must equal ("bar")
      page.date.get.getYear must equal (2000)
      page.date.get.getMonthOfYear must equal (01)
    }
    */
  }

}
