package com.ayosec.wikispray

import org.scalatest._
import web._
import persistence.Page

import akka.testkit._
import akka.actor.ActorSystem
import org.joda.time.DateTime

import cc.spray.json._
import cc.spray.test.SprayTest
import cc.spray.http.{HttpMethod, HttpRequest}
import cc.spray.http.HttpMethods.{GET, POST}

class PagesServiceSpec(_system: ActorSystem) extends TestKit(_system)
  with WordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ImplicitSender
  with SprayTest with PagesService
  with TestMongo
{

  def this() = this(ActorSystem("PagesServiceSpec"))

  def request(method: HttpMethod, uri: String) = test(HttpRequest(method, uri)) { routes }

  "An anonymous user" should {
    "see a page" in {
      val pageId = storePage(Page("A summary", "γειά σου", new DateTime(2010, 10, 20, 0, 0)))

      val response = request(GET, "/pages/" + pageId).response
      response.status.value must equal (200)

      response.content flatMap { _.contentType.charset } map { _.value } must equal (Some("UTF-8"))
      response.content map { _.contentType.mediaType.value } must equal (Some("application/json"))

      val page = response.content.as[String].right.get.asJson.asJsObject.fields
      page("summary") must equal (JsString("A summary"))
      page("content") must equal (JsString("γειά σου"))
      page("date").toString must startWith ("\"2010-10-20")
    }

    "not be able to create a page" in {
      pending
    }
  }

  "A valid user" should {
    "be able to create a new page" in {
      pending
    }

    "be able to update an existing page" in {
      pending
    }

    "be able to delete a page" in {
      pending
    }
  }

}
