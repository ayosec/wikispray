package com.ayosec.wikispray.web

import cc.spray._
import cc.spray.json._
import cc.spray.http.{HttpResponse, StatusCodes}
import cc.spray.typeconversion._

import com.ayosec.wikispray.persistence._

import akka.pattern.ask
import akka.dispatch.Promise
import akka.actor.{ActorSystem, Actor, Props}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import cc.spray.AuthenticationFailedRejection

trait PagesService extends Directives with SprayJsonSupport {

  val system: ActorSystem

  lazy val persistenceActor = system.actorOf(Props[PersistenceActor])

  val routes = {

    import PageJsonProtocol._

    implicit val timeout = akka.util.Timeout(3000)

    pathPrefix("pages") {
      path("[0123456789abcdefABCDEF]{24}".r) { pageId =>
        get {
          withPage(pageId) { page =>
            // Read the page
            completeWith { page }
          }
        } ~
        hasUser { user =>
          formFields('summary ?, 'content ?, 'date ?) { (summary, content, date) =>
            post { ctx =>
              // Update the page
              val newDate = date map { d => new DateTime(d) }
              ask(persistenceActor, UpdatePage(new ObjectId(pageId), summary, content, newDate)) map {
                case PageUpdated => ctx.complete("Ok")
                case x => ctx.complete(HttpResponse(StatusCodes.NotFound))
              }
            }
          } ~
          delete { ctx =>
            ask(persistenceActor, DeletePage(new ObjectId(pageId))) map {
              case PageDeleted => ctx.complete("Ok")
              case x => ctx.complete(HttpResponse(StatusCodes.NotFound))
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

  lazy val hasUser = authenticate(
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

  def withPage(id: String): (Page => Route) => Route = { route => ctx =>
    implicit val timeout = akka.util.Timeout(3000)

    ask(
      persistenceActor,
      LoadPage(new ObjectId(id))
    ) map {
      case LoadedPage(page) =>
        route(page)(ctx)

      case PageNotFound =>
        ctx complete HttpResponse(StatusCodes.NotFound)
    }
  }

}
