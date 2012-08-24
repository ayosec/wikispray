package com.ayosec.wikispray.persistence

import akka.actor.Actor
import com.mongodb.DBObject
import com.mongodb.WriteConcern.SAFE
import org.bson.types.ObjectId

// Requests
case class Connect(uri: String)
case class DropCollection(collection: String)
case class InsertDocument(collection: String, document: DBObject)
case class LoadDocument(collection: String, query: DBObject)

// Responses
case class Connected(uri: String)
case class CollectionDropped(collection: String)

case class Inserted(id: ObjectId)
case class DocumentLoaded(document: DBObject)
case class DocumentNotFound(query: DBObject)

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

    case LoadDocument(collection, query) =>
      db.getCollection(collection).findOne(query) match {
        case null =>
          sender ! DocumentNotFound(query)

        case result: DBObject =>
          sender ! DocumentLoaded(result)
      }
  }

}
