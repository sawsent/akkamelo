package com.akkamelo.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor.RegisterClientActor
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{Configure, SayHello}
import com.akkamelo.api.endpoint.Server
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("no-conf")
  implicit val ec = system.dispatcher

  val config = ConfigFactory.load()
  implicit val clientActorResolverTimeout = Timeout(1.second)

  val greeter = system.actorOf(GreeterActor.props, "greeter")
  greeter ! Configure(config.getString("boot.message"))
  greeter ! SayHello

  val clientSupervisor = system.actorOf(ClientActorSupervisor.props(
    (id: Int) => s"${config.getString("actor.client.name.prefix")}$id${config.getString("actor.client.name.suffix")}"
  ), "client-actor-supervisor")

  1 to 5 foreach { id =>
    clientSupervisor ? RegisterClientActor(id)
  }

  val server: Server = Server("localhost", 8080, clientSupervisor).start()

  scala.io.StdIn.readLine()
  server.close()

}


