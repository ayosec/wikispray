package com.ayosec.wikispray.moon

import akka.dispatch.ExecutionContext
import akka.actor.ActorSystem

import collection.JavaConversions._

object MoonDB {
  def apply(uri: String) = new MoonDB(uri, ActorSystem())
  def apply(uri: String, system: ActorSystem) = new MoonDB(uri, system)
}

class MoonDB private (val uri: String, implicit val system: ActorSystem) {

  // We will use the same connection to the Mongo database.
  // The driver is thread-safe.

  val dbConnection = new com.mongodb.MongoURI(uri).connectDB()

  // Get a MongoCollection instance
  def apply(collectionName: String) = new MoonCollection(this, dbConnection.getCollection(collectionName))

  def collections = dbConnection.getCollectionNames().iterator map { apply(_) }

}
