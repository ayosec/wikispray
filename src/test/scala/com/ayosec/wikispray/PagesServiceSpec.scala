package com.ayosec.wikispray

import org.scalatest._
import web._
import persistence._

import akka.testkit._
import akka.actor.ActorSystem
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
  with TestMongo
{

  def this() = this(ActorSystem("PagesServiceSpec"))

  override val actorSystem = system

  def request(
    method: HttpMethod,
    uri: String,
    params: Option[Map[String, String]] = None,
    headers: List[HttpHeader] = List()
  ) = {
    test(HttpRequest( method, uri, content = params map { _.toHttpContent }, headers = headers)) { routes }
  }

  def newPageId() = storePage(Page("A summary", "γειά σου", new DateTime(2010, 10, 20, 0, 0)))

  "An anonymous user" should {
    "see a page" in {

      val response = request(GET, "/pages/" + newPageId()).response
      response.status.value must equal (200)

      response.content flatMap { _.contentType.charset } map { _.value } must equal (Some("UTF-8"))
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

      persistenceActor ! LoadPage(pageId)
      expectMsgClass(classOf[LoadedPage])
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
      persistenceActor ! LoadLastPage
      val newPage = expectMsgClass(classOf[LoadedPage]).page
      newPage.summary must equal ("a summary")
      newPage.content must equal ("more content")
      newPage.date.getYear must equal (2010)
    }

    "be able to update an existing page" in {
      val pageId = newPageId()

      val result = request(POST, "/pages/" + pageId,
        params = Some(Map("summary" -> "new summary", "content" -> "new content")),
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.handled must be (true)
      result.response.status.value must equal (200)

      // Check if the page was updated
      persistenceActor ! LoadPage(pageId)
      val newPage = expectMsgClass(classOf[LoadedPage]).page
      newPage.summary must equal ("new summary")
      newPage.content must equal ("new content")
      newPage.date.getYear must equal (2010)
    }

    "see a 404 when the updated page does not exist" in {
      val result = request(POST, "/pages/000000000000000000000000",
        params = Some(Map("summary" -> "new summary")),
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.response.status.value must equal (404)
    }

    "be able to delete a page" in {
      val pageId = newPageId()

      persistenceActor ! LoadPage(pageId)
      expectMsgClass(classOf[LoadedPage])

      val result = request(DELETE, "/pages/" + pageId,
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.handled must be (true)
      result.response.status.value must equal (200)

      // Check if the page was updated
      persistenceActor ! LoadPage(pageId)
      expectMsg(PageNotFound)
    }

    "see a 404 when the delete page does not exist" in {
      val result = request(DELETE, "/pages/000000000000000000000000",
        headers = List(Authorization(BasicHttpCredentials("admin", "pw"))))

      result.response.status.value must equal (404)
    }
  }

}
