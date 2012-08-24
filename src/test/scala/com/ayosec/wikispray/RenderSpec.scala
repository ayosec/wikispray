
package com.ayosec.wikispray

import org.scalatest._
import akka.testkit._
import akka.actor._

class RenderSpec(_system: ActorSystem) extends TestKit(_system)
  with ImplicitSender
  with WordSpec
  with MustMatchers
  with BeforeAndAfterAll
{

  def this() = this(ActorSystem("RenderSpec"))

  override def afterAll { system.shutdown() }

  import render._

  "A Render actor" must {
    "return HTML source" in {

      val render = TestActorRef[RenderActor]
      render ! MarkdownSource("This *is* a **test**")
      expectMsg(HTMLContent("<p>This <em>is</em> a <strong>test</strong></p>"))
    }

  }
}
