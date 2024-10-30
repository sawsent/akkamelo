package com.akkamelo.api

import akka.actor.ActorSystem
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{Configure, SayHello}
import com.akkamelo.api.endpoint.Server
import com.typesafe.config.ConfigFactory

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("no-conf")
  implicit val ec = system.dispatcher

  val config = ConfigFactory.load()

  val greeter = system.actorOf(GreeterActor.props, "greeter")
  greeter ! Configure(config.getString("boot.message"))
  greeter ! SayHello

  val server: Server = new Server("localhost", 8080)
  val closeable = server.start()

  scala.io.StdIn.readLine()
  closeable.close()

}
