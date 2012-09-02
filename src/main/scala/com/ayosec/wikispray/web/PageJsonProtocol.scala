package com.ayosec.wikispray.web

import cc.spray.json._
import org.joda.time.DateTime

object PageJsonProtocol extends DefaultJsonProtocol {

  implicit object PageJsonFormat extends RootJsonFormat[Page] {
    def write(c: Page) = JsObject(
      Map(
        "summary" -> c.summary,
        "content" -> c.content,
        "date" -> (c.date map { d => d.toString() })
      ) map {
        case (k,v) => (k, v map { JsString(_) })
      } filterNot {
        case (k,v) => v.isEmpty
      } map {
        case (k,v) => (k, v.get)
      }
    )

    def read(value: JsValue) = value match {
      // TODO
      case _ => deserializationError("Not implemented")
    }
  }

}
