package com.ayosec.wikispray.moon

import org.scalatest._
import com.osinka.subset._

import akka.dispatch.Await
import akka.dispatch.Future
import akka.util.duration._

class MoonCollectionSpec extends WordSpec
  with MustMatchers
  with BeforeAndAfterEach
{

  val moon = MoonDB("mongodb://localhost/wikispray-MoonCollectionSpec")

  // This helper method is used to get the result of a future is a short form
  def sync[T](future: Future[T]) = Await.result(future, 2 seconds)

  override def beforeEach {
    super.beforeEach()

    for(coll <- moon.collections if coll.name != "system.indexes") {
      sync(coll.drop())
    }
  }

  // Subset's objects. Used to create DBObjects
  val name = "name".fieldOf[String]
  val age = "age".fieldOf[Int]

  // Specs

  "A collection" must {
    "return the number of items" in {
      val coll = moon("things")

      sync(coll.insert(name("a") ~ age(1)))
      sync(coll.count) must be (1)

      sync(coll.insert(name("b") ~ age(2)))
      sync(coll.count) must be (2)

      sync(coll.count(name("a"))) must be (1)
      sync(coll.count(name("foo"))) must be (0)
    }

    "be dropped after a drop" in {
      val coll = moon("things")

      sync(coll.insert(name("a")))
      sync(coll.count) must be (1)

      sync(coll.drop)
      sync(coll.count) must be (0)
    }

    "get a document by its id" in {
      val coll = moon("things")

      val id = sync(coll.insert(name("foo") ~ age(10)))

      val doc = sync(coll.findById(id))

      doc.read[String]("name") must be (Some("foo"))
      doc.read[Int]("age") must be (Some(10))
      doc.read[Int]("none") must be (None)
    }

    "fail when the document does not exist" in {
      val coll = moon("things")
      sync(
        coll.findById("000000000000000000000000") map { doc => false } recover { case _: DocumentNotFound => true }
      ) must be (true)
    }

    "load a document and modify it" in {
      val coll = moon("things")
      val id = sync(coll.insert(name("foo") ~ age(1)))

      sync(coll.count(name("foo"))) must be (1)

      val doc = sync(coll.findById(id))
      doc.write("name", "bar")
      doc.read[String]("name") must be (Some("bar"))

      sync(coll.count(name("foo"))) must be (1) // Value in the database has to be the same

      sync(doc.save()) must be (true)

      sync(coll.count(name("foo"))) must be (0)
      sync(coll.count(name("bar"))) must be (1)
    }

    "return the last document" in {
      val coll = moon("things")

      // Create some documents
      val docA = sync(coll.insert(name("a")))
      val docB = sync(coll.insert(name("b")))
      val docC = sync(coll.insert(name("c")))

      // and modify one of them (natural sort will be different)
      val doc = sync(coll.findById(docB))
      doc.write("age", 10)
      sync(doc.save())

      // Finally, the last document (by id) should be docC
      sync(coll.last()).read[String]("name") must be (Some("c"))
    }

    "return an error when there is no last page" in {
      val coll = moon("things")
      sync(coll.count) must be (0)
      sync(
        coll.last() map { doc => false } recover { case _: DocumentNotFound => true }
      ) must be (true)
    }

    "create a document from a moon collection" in {
      val coll = moon("things")
      val doc = coll.build()

      sync(coll.count(name("new name"))) must be (0)

      doc.write("name", "new name")
      sync(doc.save()) must be (true)

      sync(coll.count(name("new name"))) must be (1)
    }

    "delete an existing document" in {
      val coll = moon("things")
      val id = sync(coll.insert(name("a") ~ age(1)))

      val doc = sync(coll.findById(id))
      sync(doc.destroy()) must be (true)
    }
  }
}
