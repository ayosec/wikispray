package com.ayosec.wikispray.web

import cc.spray._

import com.ayosec.wikispray.persistence._

import akka.pattern.ask
import akka.actor.{ActorSystem, Actor, Props}
import org.bson.types.ObjectId

trait PagesService extends Directives with JsonSupport {

  val system: ActorSystem

  lazy val persistenceActor = system.actorOf(Props[PersistenceActor])

  val routes = {

    import PageJsonProtocol._

    implicit val timeout = akka.util.Timeout(3000)

    pathPrefix("pages") {
      path("[0123456789abcdefABCDEF]{24}".r) { pageId =>
        get {
          _.complete(
            ask(persistenceActor, LoadPage(new ObjectId(pageId))) map {
              case LoadedPage(page) => page
            }
          )
        }
      }
    }
  }
}
