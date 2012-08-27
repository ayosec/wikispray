package com.ayosec.wikispray.persistence

import akka.actor.Actor
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._

import com.mongodb.DBObject
import com.mongodb.BasicDBObject
import org.bson.types.ObjectId
import org.joda.time.DateTime

import com.osinka.subset._
import JodaValues._

// Page store in the database
case class Page(summary: String, content: String, date: DateTime)

// Requests to the PersistenceActor
case class StorePage(page: Page)
case class LoadPage(id: ObjectId)
case object LoadLastPage

// Response
case class StoredPage(page: Page, id: ObjectId)
case class LoadedPage(page: Page)
case class PageNotFound(id: ObjectId)

class PersistenceActor extends Actor {

  implicit val timeout = Timeout(3 seconds)

  final val id = "_id".fieldOf[ObjectId]
  final val summary = "summary".fieldOf[String]
  final val content = "content".fieldOf[String]
  final val date = "date".fieldOf[DateTime]

  val collection = "pages"

  def save(page: Page, requester: ActorRef) = {
    val dbobject: DBObject =
      summary(page.summary) ~
      content(page.content) ~
      date(page.date)

    ask(
      context.system.actorFor("/user/mongo"),
      InsertDocument(collection, dbobject)
    ) onSuccess {
      case Inserted(id) => requester ! StoredPage(page, id)
    }
  }

  def load(pageId: ObjectId, requester: ActorRef) = {
    ask(
      context.system.actorFor("/user/mongo"),
      LoadDocument(collection, query = Some((id === pageId) :DBObject))
    ) onSuccess {
      case DocumentNotFound =>
        requester ! PageNotFound(pageId)

      case DocumentLoaded(summary(s) ~ content(c) ~ date(d)) =>
        requester ! LoadedPage(Page(s, c, d))
    }
  }

  def loadLast(requester: ActorRef) = {
    ask(
      context.system.actorFor("/user/mongo"),
      LoadDocument(collection, sort = Some(new BasicDBObject {{ put("_id", -1) }}))
    ) onSuccess {
      case DocumentNotFound =>
        requester ! PageNotFound(new ObjectId("000000000000000000000000"))

      case DocumentLoaded(summary(s) ~ content(c) ~ date(d)) =>
        requester ! LoadedPage(Page(s, c, d))
    }
  }

  def receive = {
    case StorePage(page) => save(page, sender)
    case LoadPage(id) => load(id, sender)
    case LoadLastPage => loadLast(sender)
  }
}
