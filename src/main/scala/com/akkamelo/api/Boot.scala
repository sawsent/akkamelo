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

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("no-conf")
  implicit val ec = system.dispatcher

  val config = ConfigFactory.load()


  val greeter = system.actorOf(GreeterActor.props, "greeter")
  greeter ! Configure(config.getString("boot.message"))
  greeter ! SayHello

  implicit val clientActorSupervisorTimeout = Timeout(config.getLong("actor.client.supervisor.timeoutSeconds"), TimeUnit.SECONDS)
  val clientNamePrefix = config.getString("actor.client.name.prefix")
  val clientNameSuffix = config.getString("actor.client.name.suffix")
  val clientSupervisor = system.actorOf(ClientActorSupervisor.props(
    (id: Int) => clientNamePrefix + id.toString + clientNameSuffix
  ), "client-actor-supervisor")

  val initialClients = config.getObjectList("boot.initialClients")

  initialClients.forEach(client => {
    val id = client.get("id").unwrapped().asInstanceOf[Int]
    clientSupervisor ? RegisterClientActor(id, client.get("initialBalance").unwrapped().asInstanceOf[Int], client.get("limit").unwrapped().asInstanceOf[Int])
  })

  val server: Server = Server("localhost", 8080, clientSupervisor).start()

  scala.io.StdIn.readLine()
  server.close()

}


