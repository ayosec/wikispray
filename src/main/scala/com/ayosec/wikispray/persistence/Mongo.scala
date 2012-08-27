package com.ayosec.wikispray.persistence

import akka.actor.Actor
import com.mongodb.DBObject
import com.mongodb.WriteConcern.SAFE
import org.bson.types.ObjectId

// Requests
case class Connect(uri: String)
case class DropCollection(collection: String)
case class InsertDocument(collection: String, document: DBObject)
case class LoadDocument(collection: String, query: Option[DBObject] = None, sort: Option[DBObject] = None)

// Responses
case class Connected(uri: String)
case class CollectionDropped(collection: String)

case class Inserted(id: ObjectId)
case class DocumentLoaded(document: DBObject)
case object DocumentNotFound

class Mongo extends Actor {

  private[this] var db: com.mongodb.DB = _

  def receive = {
    case Connect(uri) =>
      db = new com.mongodb.MongoURI(uri).connectDB()
      sender ! Connected(uri)

    case DropCollection(collection) =>
      db.getCollection(collection).drop()
      sender ! CollectionDropped(collection)

    case InsertDocument(collection, document) =>
      db.getCollection(collection).insert(document, SAFE)
      sender ! Inserted(document.get("_id").asInstanceOf[ObjectId])

    case LoadDocument(collection, query, sort) =>
      val coll= db.getCollection(collection)

      // Cursor base, with the query
      var cursor = query match {
        case Some(q) => coll.find(q)
        case _ => coll.find()
      }

      // Apply sort criteria, if any
      sort foreach { s => cursor = cursor.sort(s) }

      cursor = cursor.limit(1)

      if(cursor.hasNext)
        sender ! DocumentLoaded(cursor.next)
      else
        sender ! DocumentNotFound
  }

}
