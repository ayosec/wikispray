package com.ayosec.wikispray.moon

import akka.dispatch.Future
import com.mongodb.DBObject
import com.mongodb.BasicDBObject
import com.mongodb.BasicDBObjectBuilder
import com.mongodb.WriteConcern.SAFE
import org.bson.types.ObjectId

class DocumentNotFound(val collection: MoonCollection, val query: DBObject) extends MoonError

class MoonCollection(val moonDB: MoonDB, val dbCollection: com.mongodb.DBCollection) {

  implicit val system = moonDB.system

  lazy val name = dbCollection.getName

  def insert(document: DBObject) = Future {
    val result = dbCollection.insert(document, SAFE)
    document.get("_id").asInstanceOf[ObjectId]
  }

  def update(query: DBObject, obj: DBObject, upsert: Boolean = false, multi: Boolean = false) = Future {
    dbCollection
      .update(query, obj, upsert, multi, SAFE)
      .getField("updatedExisting") == true
  }

  def drop() = Future { dbCollection.drop() }

  def build(dbobject: Option[BasicDBObject] = None) = {
    new MoonDocument(this, dbobject getOrElse { new BasicDBObject })
  }

  def destroy(query: DBObject) = Future {
    dbCollection.remove(query, SAFE).getField("n") == 1
  }

  def count = Future { dbCollection.count() }
  def count(query: DBObject) = Future { dbCollection.count(query) }

  def findById(id: String): Future[MoonDocument] = findById(new ObjectId(id))

  def findById(id: ObjectId): Future[MoonDocument] = Future {
    val query = new BasicDBObjectBuilder().add("_id", id).get
    dbCollection.findOne(query) match {
      case null => throw new DocumentNotFound(this, query)
      case doc: DBObject => new MoonDocument(this, doc)
    }
  }
}
