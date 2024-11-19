package com.akkamelo.api

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.akkamelo.api.Boot.{config, ec, system}
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor.RegisterClientActor
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{Configure, SayHello}
import com.akkamelo.api.actor.persistencetest.PersistentTestActor
import com.akkamelo.api.endpoint.{PersistenceTestServer, Server}
import com.typesafe.config.{Config, ConfigFactory}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext

object Boot extends App {
  val config: Config = ConfigFactory.load()
  val systemName: String = config.getString("boot.system.name")

  implicit val system: ActorSystem = ActorSystem(systemName, config)
  implicit val ec: ExecutionContext = system.dispatcher
  val materializer = Materializer(system)

  val booter: Booter = Booter(system, ec, materializer, config)
  booter.greet()

  val clientActorSupervisor = booter.getClientActorSupervisor()
  booter.registerInitialClients(clientActorSupervisor)
  val server: Option[Server] = booter.getServer(clientActorSupervisor)
  val persistenceTestServer: Option[PersistenceTestServer] = booter.getPersistenceTestServer()

}

object Booter {
  def apply(system: ActorSystem, ec: ExecutionContext, materializer: Materializer, config: Config): Booter = new Booter(system, ec, materializer, config)
}
class Booter(val system: ActorSystem, val ec: ExecutionContext, val materializer: Materializer, val config: Config) {

  def getClientActorSupervisor(): ActorRef = {
    val clientNamePrefix = config.getString("actor.client.name.prefix")
    val clientNameSuffix = config.getString("actor.client.name.suffix")

    system.actorOf(ClientActorSupervisor.props(
      (id: Int) => clientNamePrefix + id.toString + clientNameSuffix
    ), "client-actor-supervisor")
  }

  def registerInitialClients(clientSupervisor: ActorRef): Unit = {
    val initialClients = config.getObjectList("boot.initialClients")
    implicit val clientRegisterTimeout: Timeout = Timeout(config.getLong("boot.initial-register.timeoutSeconds"), TimeUnit.SECONDS)

    initialClients.forEach(client => {
      val id = client.get("id").unwrapped().asInstanceOf[Int]
      clientSupervisor ? RegisterClientActor(id, client.get("initialBalance").unwrapped().asInstanceOf[Int], client.get("limit").unwrapped().asInstanceOf[Int])
    })

  }

  def getServer(clientActorSupervisor: ActorRef): Option[Server] = {
    val serverEnabled = config.getBoolean("server.enabled")
    if (serverEnabled) {
      Some(startServer(clientActorSupervisor))
    } else {
      None
    }
  }

  def startServer(clientActorSupervisor: ActorRef): Server = {
    val actorResolveTimeout: Timeout = Timeout(config.getLong("actor.client.supervisor.timeoutSeconds"), TimeUnit.SECONDS)
    val host: String = config.getString("server.host")
    val port: Int = config.getInt("server.port")
    Server.newStartedAt(host, port, clientActorSupervisor)(system, ec, actorResolveTimeout)
  }

  def greet(): Unit = {
    val greeter = system.actorOf(GreeterActor.props, "greeter")
    greeter ! Configure(config.getString("boot.message"))
    greeter ! SayHello
  }

  def getPersistenceTestServer(): Option[PersistenceTestServer] = {
    val persistenceTestServerEnabled = config.getBoolean("persistence-test-server.enabled")
    if (persistenceTestServerEnabled) {
      Some(startPersistenceTestServer())
    } else {
      None
    }
  }
  def startPersistenceTestServer(): PersistenceTestServer = {
    val persistenceTestActor = system.actorOf(PersistentTestActor.props("persistence-test"), "persistence-test-actor")
    val persistenceTestHost: String = config.getString("persistence-test-server.host")
    val persistenceTestPort: Int = config.getInt("persistence-test-server.port")
    PersistenceTestServer(persistenceTestHost, persistenceTestPort, persistenceTestActor)(system, materializer, ec)
  }
}