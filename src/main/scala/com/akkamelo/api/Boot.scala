package com.akkamelo.api

import akka.actor.ActorSystem
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{Configure, SayHello}
import com.typesafe.config.ConfigFactory

object Boot extends App {
  val system: ActorSystem = ActorSystem("no-conf")
  val config = ConfigFactory.load()

  val greeter = system.actorOf(GreeterActor.props, "greeter")
  greeter ! Configure(config.getString("boot.message"))
  greeter ! SayHello

}
