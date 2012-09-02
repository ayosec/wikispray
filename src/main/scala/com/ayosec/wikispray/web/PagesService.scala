package com.ayosec.wikispray.web

import cc.spray._
import cc.spray.json._
import cc.spray.http.{HttpResponse, StatusCodes}
import cc.spray.typeconversion._

import akka.pattern.ask
import akka.dispatch.Promise
import akka.actor.{ActorSystem, Actor, Props}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import cc.spray.AuthenticationFailedRejection

import com.ayosec.wikispray.moon.MoonDB
import com.ayosec.wikispray.moon.MoonDocument
import com.ayosec.wikispray.moon.DocumentNotFound

trait PagesService extends Directives with SprayJsonSupport {

  implicit val actorSystem: ActorSystem

  val moon: MoonDB

  lazy val pages = moon("pages")

  val routes = {

    import PageJsonProtocol._

    implicit val timeout = akka.util.Timeout(3000)

    pathPrefix("pages") {
      path("[0123456789abcdefABCDEF]{24}".r) { pageId =>
        withPage(pageId) { page =>
          get {
            // Read the page
            completeWith { page }
          } ~
          hasUser { user =>
            formFields('summary ?, 'content ?, 'date ?) { (summary, content, date) =>
              post { ctx =>
                // Update the page

                summary foreach { s => page.summary = s }
                content foreach { c => page.content = c }
                date    foreach { d => page.date = new DateTime(d) }

                page.save() map { result => ctx complete { if(result) "Ok" else "Fail" } }
              }
            } ~
            delete { ctx =>
              // Delete the page
              page.destroy() map { result => ctx complete { if(result) "Ok" else "Fail" } }
            }
          }
        }
      } ~
      post {
        hasUser { user =>
          formFields('summary, 'content, 'date) { (summary, content, date) =>
            val doc = pages.build()
            doc.write("summary", summary)
            doc.write("content", content)
            doc.write("date", new DateTime(date).toString())

            completeWith {
              doc.save() map { id => Map("id" -> doc.id.toString).toJson.toString }
            }
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

    pages.findById(id) map { document =>
      route(document.mutate(new Page(_)))(ctx)
    } recover {
      case _: DocumentNotFound => 
        ctx complete HttpResponse(StatusCodes.NotFound, content = "Object not found")
    }
  }

}
