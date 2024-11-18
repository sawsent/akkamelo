package com.akkamelo.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor.RegisterClientActor
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{Configure, SayHello}
import com.akkamelo.api.actor.persistencetest.PersistentTestActor
import com.akkamelo.api.endpoint.{PersistenceTestServer, Server}
import com.typesafe.config.ConfigFactory

import java.util.concurrent.TimeUnit

object Boot extends App {
  val config = ConfigFactory.load()

  implicit val system: ActorSystem = ActorSystem("no-conf")
  implicit val ec = system.dispatcher

  val greeter = system.actorOf(GreeterActor.props, "greeter")
  greeter ! Configure(config.getString("boot.message"))
  greeter ! SayHello

  val persistenceTestActor = system.actorOf(PersistentTestActor.props("persistence-test"), "persistence-test-actor")

  // Persistence testing
  val persistenceTestHost: String = config.getString("persistence-test-server.host")
  val persistenceTestPort: Int = config.getInt("persistence-test-server.port")
  val persistenceTestServer = PersistenceTestServer(persistenceTestHost, persistenceTestPort, persistenceTestActor)

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

  val host: String = config.getString("server.host")
  val port: Int = config.getInt("server.port")
  // val server: Server = Server.newStartedAt(host, port, clientSupervisor)


}


