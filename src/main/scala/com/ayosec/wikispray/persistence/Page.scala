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
case class DeletePage(id: ObjectId)
case class UpdatePage(id: ObjectId, summary: Option[String], content: Option[String], date: Option[DateTime])
case object LoadLastPage

// Response
case class StoredPage(page: Page, id: ObjectId)
case class LoadedPage(page: Page)
case object PageNotFound
case object PageUpdated
case object PageDeleted

class PersistenceActor extends Actor {

  implicit val timeout = Timeout(3 seconds)

  object fields {
    final val id = "_id".fieldOf[ObjectId]
    final val summary = "summary".fieldOf[String]
    final val content = "content".fieldOf[String]
    final val date = "date".fieldOf[DateTime]
  }

  final val collection = "pages"

  def save(page: Page, requester: ActorRef) = {
    val dbobject: DBObject =
      fields.summary(page.summary) ~
      fields.content(page.content) ~
      fields.date(page.date)

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
      LoadDocument(collection, query = Some((fields.id === pageId) :DBObject))
    ) onSuccess {
      case DocumentNotFound =>
        requester ! PageNotFound

      case DocumentLoaded(fields.summary(s) ~ fields.content(c) ~ fields.date(d)) =>
        requester ! LoadedPage(Page(s, c, d))
    }
  }

  def loadLast(requester: ActorRef) = {
    ask(
      context.system.actorFor("/user/mongo"),
      LoadDocument(collection, sort = Some(new BasicDBObject {{ put("_id", -1) }}))
    ) onSuccess {
      case DocumentNotFound =>
        requester ! PageNotFound

      case DocumentLoaded(fields.summary(s) ~ fields.content(c) ~ fields.date(d)) =>
        requester ! LoadedPage(Page(s, c, d))
    }
  }

  def update(pageId: ObjectId, summary: Option[String], content: Option[String], date: Option[DateTime], requester: ActorRef) = {
    val changes: DBObject = List(
      summary map { fields.summary(_) },
      content map { fields.content(_) },
      date    map { fields.date(_) }
    ) filterNot { _.isEmpty } map { _.get } reduce { _ ~ _ }

    ask(
      context.system.actorFor("/user/mongo"),
      UpdateDocument(collection, pageId, changes)
    ) onSuccess {
      case Updated => requester ! PageUpdated
      case DocumentNotFound => requester ! PageNotFound
    }
  }

  def delete(pageId: ObjectId, requester: ActorRef) = {
    ask(
      context.system.actorFor("/user/mongo"),
      DeleteDocument(collection, pageId)
    ) onSuccess {
      case Deleted => requester ! PageDeleted
      case DocumentNotFound => requester ! PageNotFound
    }
  }

  def receive = {
    case StorePage(page) => save(page, sender)
    case LoadPage(id) => load(id, sender)
    case LoadLastPage => loadLast(sender)
    case UpdatePage(id, summary, content, date) => update(id, summary, content, date, sender)
    case DeletePage(id) => delete(id, sender)
  }
}
