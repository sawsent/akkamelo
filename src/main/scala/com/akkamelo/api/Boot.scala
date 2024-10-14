package com.akkamelo.api

import akka.actor.ActorSystem
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.SayHello

object Boot extends App {
  val system: ActorSystem = ActorSystem("no-conf")

  val greeter = system.actorOf(GreeterActor.props("Hello World"), "greeter")
  greeter ! SayHello

}
