package com.ayosec.wikispray.persistence

object Mongo {

  lazy val connection = new com.mongodb.Mongo;
  lazy val db = connection.getDB("wikispray-test");

  def collection(name: String) = db.getCollection(name)

}
