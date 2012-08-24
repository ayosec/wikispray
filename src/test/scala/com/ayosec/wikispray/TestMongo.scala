package com.ayosec.wikispray

import persistence._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach

trait TestMongo { this: BeforeAndAfterAll with BeforeAndAfterEach =>

  override def beforeAll {
    Mongo.connect("mongodb://localhost/wikispray-test")
  }

  // Truncate pages collections before every test
  override def beforeEach {
    Mongo.collection("pages").drop()
  }

}
