package com.ayosec.wikispray.web

import cc.spray.json._
import org.joda.time.DateTime

object PageJsonProtocol extends DefaultJsonProtocol {

  implicit object DateTimeJsonFormat extends JsonFormat[DateTime] {
    def write(dt: DateTime) = JsString(dt.toString())
    def read(value: JsValue) = value match {
      case JsString(dt) => new DateTime(dt)
      case dt => deserializationError("Expected DateTime as JsString, but got " + dt)
    }
  }

  implicit val pageFormat = jsonFormat3(Page)

}
