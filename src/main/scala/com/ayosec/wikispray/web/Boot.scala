package com.ayosec.wikispray.web

import com.ayosec.wikispray.persistence._
import cc.spray.io.pipelines.MessageHandlerDispatch
import cc.spray.io.IoWorker
import cc.spray.can.server.HttpServer
import cc.spray._
import akka.actor._

object Boot extends App {
  // we need an ActorSystem to host our application in
  val system = ActorSystem("Wikispray")

  system.actorOf(Props[Mongo], name = "mongo") ! Connect("mongodb://localhost/wikispray-devel")

  val service = system.actorOf(
    props = Props(new HttpService(new PagesService { val actorSystem = system }.routes)),
    name = "pages"
  )

  val rootService = system.actorOf(
    props = Props(new SprayCanRootService(service)),
    name = "spray-root-service"
  )

  val ioWorker = new IoWorker(system).start()
  val server = system.actorOf(
    props = Props(new HttpServer(ioWorker, MessageHandlerDispatch.SingletonHandler(rootService))),
    name = "http-server"
  )

  server ! HttpServer.Bind("localhost", 8080)

  system.registerOnTermination {
    ioWorker.stop()
  }
}
