package com.akkamelo.api.endpoint

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

object HelloWorld extends App {
  implicit val system = ActorSystem("hello-world")
  implicit val executionContext = system.dispatcher

  var messages = List.empty[String]

  val route: Route = {
    concat(
      path("messages") {
        get {
          complete(messages.toString())
        }
      },
      post {
        path("messages") {
          entity(as[String]) { message =>
            messages = messages :+ message
            complete(s"Message '$message' has been stored.")
          }
        }
      }
    )

  }

  val bindingFuture = akka.http.scaladsl.Http().newServerAt("localhost", 8080).bind(route)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  scala.io.StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
