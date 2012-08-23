package com.ayosec.wikispray.persistence

import akka.actor.Actor

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.joda.time.DateTime

import com.osinka.subset._
import JodaValues._

// Page store in the database
case class Page(summary: String, content: String, date: DateTime)

// Requests to the PersistenceActor
case class StorePage(page: Page)
case class LoadPage(id: ObjectId)

// Response
case class StoredPage(page: Page, id: ObjectId)
case class LoadedPage(page: Page)
case class PageNotFound(id: ObjectId)

class PersistenceActor extends Actor {

  final val id = "_id".fieldOf[ObjectId]
  final val summary = "summary".fieldOf[String]
  final val content = "content".fieldOf[String]
  final val date = "date".fieldOf[DateTime]

  lazy val mongo = new com.mongodb.Mongo;
  lazy val db = mongo.getDB("wikispray-test");

  def save(page: Page) = {
    val dbobject: DBObject =
      summary(page.summary) ~
      content(page.content) ~
      date(page.date)

    db.getCollection("pages").insert(dbobject)
    dbobject.get("_id").asInstanceOf[ObjectId]
  }

  def load(pageId: ObjectId) = {
    db.getCollection("pages").findOne((id === pageId) :DBObject) match {
      case null => None
      case summary(s) ~ content(c) ~ date(d) => Some(Page(s, c, d))
      case _ => None
    }
  }

  def receive = {
    case StorePage(page) =>
      sender ! StoredPage(page, save(page))

    case LoadPage(id) =>
      load(id) match {
        case Some(page) => sender ! LoadedPage(page)
        case None       => sender ! PageNotFound(id)
      }
  }
}
