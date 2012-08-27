package com.ayosec.wikispray

import cc.spray.http._
import cc.spray.typeconversion._
import cc.spray.http.MediaTypes._
import cc.spray.http.HttpCharsets._
import cc.spray.typeconversion.DefaultMarshallers._

trait POSTParameters {

  implicit lazy val UnicodeFormDataMarshaller = new SimpleMarshaller[Map[String, String]] {
    val canMarshalTo = ContentType(`application/x-www-form-urlencoded`, `UTF-8`) :: Nil
    def marshal(fields: Map[String, String], contentType: ContentType) = {
      import java.net.URLEncoder.encode
      val charset = contentType.charset.getOrElse(`UTF-8`).aliases.head
      val keyValuePairs = fields.map {
        case (key, value) => encode(key, charset) + '=' + encode(value, charset)
      }
      StringMarshaller.marshal(keyValuePairs.mkString("&"), contentType)
    }
  }

}
