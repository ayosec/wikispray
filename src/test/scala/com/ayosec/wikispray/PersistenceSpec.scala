package com.ayosec.wikispray

import persistence._
import org.scalatest._
import akka.testkit._
import akka.actor._
import akka.util.Timeout
import akka.util.duration._
import org.bson.types.ObjectId
import org.joda.time.DateTime

class PersistenceSpec (_system: ActorSystem) extends TestKit(_system)
  with ImplicitSender
  with WordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with TestMongo
{

  def this() = this(ActorSystem("PersistenceSpec"))

  override def afterAll { system.shutdown() }

  implicit val timeout = Timeout(3 seconds)

  val persistenceActor = TestActorRef[PersistenceActor]

  // ====================================================
  // Specs
  // ====================================================

  "A page" should {
    "be stored in a MongoDB collection" in {

      // Store a page in the database
      persistenceActor ! StorePage(Page("the summary", "the content", new DateTime(2010, 12, 31, 00, 00)))
      val docId = expectMsgClass(classOf[StoredPage]).id

      // Then, read it
      persistenceActor ! LoadPage(docId)
      val newPage = expectMsgClass(classOf[LoadedPage]).page

      newPage.summary must equal ("the summary")
      newPage.content must equal ("the content")
      newPage.date.getYear must equal (2010)
      newPage.date.getMonthOfYear must equal (12)
      newPage.date.getDayOfMonth must equal (31)
    }
  }

  "The actor" should {
    "return a PageNotFound when the page doesn't exist" in {
      persistenceActor ! LoadPage(new ObjectId)
      expectMsgClass(classOf[PageNotFound])
    }
  }
}
