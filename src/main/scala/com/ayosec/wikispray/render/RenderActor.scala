package com.ayosec.wikispray.render

import akka.actor.Actor
import org.pegdown.PegDownProcessor

case class MarkdownSource(source: String)
case class HTMLContent(content: String)

class RenderActor extends Actor {

  def receive = {
    case MarkdownSource(content) =>
      sender ! HTMLContent(new PegDownProcessor().markdownToHtml(content))
  }

}
