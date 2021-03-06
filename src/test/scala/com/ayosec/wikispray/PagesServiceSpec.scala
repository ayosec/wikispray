package com.ayosec.wikispray

import org.scalatest._

import web._
import moon.MoonDB
import moon.MoonCollection

import akka.testkit._
import akka.actor.ActorSystem
import akka.dispatch.Await
import akka.dispatch.Future
import akka.util.duration._
import org.joda.time.DateTime

import cc.spray.json._
import cc.spray.http._
import cc.spray.typeconversion._
import cc.spray.test.SprayTest
import cc.spray.http.HttpHeaders.Authorization
import cc.spray.http.HttpMethods.{GET, POST, DELETE}
import cc.spray.AuthenticationRequiredRejection
import cc.spray.AuthorizationFailedRejection

class PagesServiceSpec(_system: ActorSystem) extends TestKit(_system)
  with WordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ImplicitSender
  with ExtraMatchers
  with POSTParameters
  with SprayTest with PagesService
{

  def this() = this(ActorSystem("PagesServiceSpec"))

  override val actorSystem = system

  implicit val moon = MoonDB("mongodb://localhost/wikispray-PagesServiceSpec")

  override def beforeEach {
    super.beforeEach()

    for(coll <- moon.collections if coll.name != "system.indexes") {
      sync(coll.drop())
    }
  }


  // This helper method is used to get the result of a future is a short form
  def sync[T](future: Future[T]) = Await.result(future, 2 seconds)

  def request(
    method: HttpMethod,
    uri: String,
    params: Option[Map[String, String]] = None,
    headers: List[HttpHeader] = List()
  ) = {
    test(HttpRequest( method, uri, content = params map { _.toHttpContent }, headers = headers)) { routes }
  }

  def newPageId() = {
    val page = Page("A summary", "γειά σου", new DateTime(2010, 10, 20, 0, 0))
    sync(page.save())
    page.id
  }

  "An anonymous user" should {
    "see a page" in {

      val response = request(GET, "/pages/" + newPageId()).response
      response.status.value must equal (200)

      //response.content flatMap { _.contentType.charset } map { _.value } must equal (Some("UTF-8"))
      response.content map { _.contentType.mediaType.value } must equal (Some("application/json"))

      val page = response.content.as[String].right.get.asJson.asJsObject.fields
      page("summary") must equal (JsString("A summary"))
      page("content") must equal (JsString("γειά σου"))
      page("date").toString must startWith ("\"2010-10-20")
    }

    "not be able to create a page" in {
      val result = request(POST, "/pages")
      result.handled must be (false)
      result.rejections.head must beOfType[AuthenticationRequiredRejection]
    }

    "not be able to modify a page" in {
      val pageId = newPageId()

      val result = request(POST, "/pages/" + pageId,
        params = Some(Map("summary" -> "new summary", "content" -> "new content")))

      result.handled must be (false)
      result.rejections.head must beOfType[AuthenticationRequiredRejection]
    }

    "not be able to delete a page" in {
      val pageId = newPageId()
      val result = request(DELETE, "/pages/" + pageId)

      result.handled must be (false)

      sync(pages.findById(pageId)) must not (beOfType[MoonCollection])
    }

    "see a 404 when the page does not exist" in {
      val response = request(GET, "/pages/000000000000000000000000").response
      response.status.value must equal (404)
    }
  }

  "A valid user" should {
    "be able to create a new page" in {
      val result = request(POST, "/pages",
        params = Some(Map("summary" -> "a summary", "content" -> "more content", "date" -> "2010-12-30T10:50:59Z")),
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.handled must be (true)

      // Check if the page was created
      val doc = sync(pages.last())
      doc.read[String]("summary").get must equal ("a summary")
      doc.read[String]("content").get must equal ("more content")
      new DateTime(doc.read[String]("date").get).getYear must equal (2010)
    }

    "be able to update an existing page" in {
      val pageId = newPageId()

      val result = request(POST, "/pages/" + pageId,
        params = Some(Map("summary" -> "new summary", "content" -> "new content")),
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.handled must be (true)
      result.response.status.value must equal (200)

      // Check if the page was updated
      val doc = sync(pages.findById(pageId))
      doc.read[String]("summary").get must equal ("new summary")
      doc.read[String]("content").get must equal ("new content")
      new DateTime(doc.read[String]("date").get).getYear must equal (2010)
    }

    "see a 404 when the updated page does not exist" in {
      val result = request(POST, "/pages/000000000000000000000000",
        params = Some(Map("summary" -> "new summary")),
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.response.status.value must equal (404)
    }

    "be able to delete a page" in {
      val pageId = newPageId()

      sync(pages.count) must be (1)

      val result = request(DELETE, "/pages/" + pageId,
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.handled must be (true)
      result.response.status.value must equal (200)

      sync(pages.count) must be (0)
    }

    "see a 404 when the delete page does not exist" in {
      val result = request(DELETE, "/pages/000000000000000000000000",
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.response.status.value must equal (404)
    }
  }

}
