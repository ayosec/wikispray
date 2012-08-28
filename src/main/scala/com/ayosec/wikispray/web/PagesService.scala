package com.ayosec.wikispray.web

import cc.spray._
import cc.spray.json._

import com.ayosec.wikispray.persistence._

import akka.pattern.ask
import akka.dispatch.Promise
import akka.actor.{ActorSystem, Actor, Props}
import org.bson.types.ObjectId
import org.joda.time.DateTime

trait PagesService extends Directives with JsonSupport {

  val system: ActorSystem

  lazy val persistenceActor = system.actorOf(Props[PersistenceActor])

  val routes = {

    import PageJsonProtocol._

    implicit val timeout = akka.util.Timeout(3000)

    pathPrefix("pages") {
      path("[0123456789abcdefABCDEF]{24}".r) { pageId =>
        get {
          // Read the page
          _.complete(
            ask(persistenceActor, LoadPage(new ObjectId(pageId))) map {
              case LoadedPage(page) => page
            }
          )
        } ~
        post {
          hasUser { user =>
            formFields('summary ?, 'content ?, 'date ?) { (summary, content, date) =>
              // Update the page
              val newDate = date map { d => new DateTime(d) }
              completeWith {
                ask(persistenceActor, UpdatePage(new ObjectId(pageId), summary, content, newDate)) map {
                  case Updated => "Ok"
                  case DocumentNotFound => "NO"
                }
              }
            }
          }
        }
      } ~
      post {
        hasUser { user =>
          formFields('summary, 'content, 'date) { (summary, content, date) =>
            // Create a new page
            _.complete(
              ask(persistenceActor, StorePage(Page(summary, content, new DateTime(date)))) map {
                case StoredPage(_, id) => Map("id" -> id.toString).toJson.toString
              }
            )
          }
        }
      }
    }
  }

  def hasUser = authenticate(
    httpBasic(
      authenticator = { user =>
        Promise.successful(
          user match {
            case Some(("admin", "pw")) => Some("user")
            case _ => None
          }
        )
      }
    )
  )
}
