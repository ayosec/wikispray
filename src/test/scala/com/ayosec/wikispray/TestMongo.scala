package com.ayosec.wikispray

import persistence._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite

import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.Timeout
import akka.dispatch.Await
import akka.util.duration._
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.ImplicitSender
import akka.pattern.ask

trait TestMongo { this: BeforeAndAfterAll with BeforeAndAfterEach with TestKit with ImplicitSender with Suite =>

  def mongo = system.actorFor("/user/mongo")

  override def beforeAll {
    system.actorOf(Props[Mongo], name = "mongo")

    mongo ! Connect("mongodb://localhost/wikispray-test-" + suiteName)
    expectMsgClass(classOf[Connected])
  }

  override def beforeEach {
    // Truncate pages collections before every test
    mongo ! DropCollection("pages")
    expectMsg(CollectionDropped("pages"))
  }

  def storePage(page: Page) = {
    implicit val timeout = Timeout(1 second)

    val persistenceActor = TestActorRef[PersistenceActor]
    Await.result((persistenceActor ? StorePage(page)), 1 second).asInstanceOf[StoredPage].id
  }

}
