package com.ayosec.wikispray.moon

import akka.dispatch.Future
import com.mongodb.DBObject
import com.mongodb.BasicDBObject
import com.mongodb.BasicDBObjectBuilder
import org.bson.types.ObjectId
import collection.JavaConversions._

case class DocumentContext(collection: MoonCollection, dbObject: DBObject)

class MoonDocument(collection: MoonCollection, dbObject: DBObject) {

  def this(context: DocumentContext) = this(context.collection, context.dbObject)

  private lazy val changesToSet = new BasicDBObject

  def read[T](attrName: String) = Option(dbObject.get(attrName).asInstanceOf[T])

  def write[T](attrName: String, value: T) = synchronized {
    changesToSet.put(attrName, value)
    dbObject.put(attrName, value)
    value
  }

  def save() = {
    val id = dbObject.get("_id").asInstanceOf[ObjectId]
    if(id == null) {
      // New document
      collection.insert(changesToSet) map { id =>
        synchronized {
          dbObject.put("_id", id)

          // Reset changes. We have to use toList since we are going to modify the changesToSet object
          for(key <- changesToSet.keySet.toList)
            changesToSet.removeField(key)

          true
        }
      }
    } else {
      collection.update(
        new BasicDBObjectBuilder().add("_id", id).get,
        new BasicDBObjectBuilder().add("$set", changesToSet).get)
    }
  }

  def id = dbObject.get("_id").asInstanceOf[ObjectId]

  def destroy() = collection.destroy(new BasicDBObjectBuilder().add("_id", id).get)


  def mutate[T](mutator: DocumentContext => T): T = mutator(DocumentContext(collection, dbObject))

}
