package com.akkamelo.api.endpoint

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._

object HelloWorld extends App {
  implicit val system = ActorSystem("hello-world")
  implicit val executionContext = system.dispatcher

  val route =
    path("hello") {
      get {
        complete("Hello, World!")
      }
    }

  val bindingFuture = akka.http.scaladsl.Http().newServerAt("localhost", 8080).bind(route)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  scala.io.StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
