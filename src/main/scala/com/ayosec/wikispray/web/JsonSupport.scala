package com.ayosec.wikispray.web

import cc.spray.json._
import cc.spray.typeconversion._
import cc.spray.http._

import cc.spray.http.HttpCharsets._
import cc.spray.http.MediaTypes._

trait JsonSupport extends SprayJsonSupport {

  implicit override def sprayJsonMarshaller[A :RootJsonWriter] = new SimpleMarshaller[A] {
    val canMarshalTo = ContentType(`application/json`, `UTF-8`) :: Nil

    lazy val printer = CompactPrinter

    def marshal(value: A, contentType: ContentType) = {
      val json = value.toJson
      val jsonSource = printer(json)
      DefaultMarshallers.StringMarshaller.marshal(jsonSource, contentType)
    }
  }
}
