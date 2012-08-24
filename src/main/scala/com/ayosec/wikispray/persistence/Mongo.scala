package com.ayosec.wikispray.persistence

object Mongo {

  private[this] var _db :com.mongodb.DB = _

  def connect(uri: String) = synchronized { _db = new com.mongodb.MongoURI(uri).connectDB() }

  def db = _db

  def collection(name: String) = _db.getCollection(name)

}
