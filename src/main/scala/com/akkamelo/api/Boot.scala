package com.akkamelo.api

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.akkamelo.api.actor.client.ClientActor
import com.akkamelo.api.actor.client.ClientActor.{ClientActorResponse, ClientAlreadyExists, ClientRegistered, RegisterClient}
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{Configure, SayHello}
import com.akkamelo.api.actor.persistencetest.PersistentTestActor
import com.akkamelo.api.http.{PersistenceTestServer, Server}
import com.akkamelo.core.logging.BaseLogging
import com.akkamelo.core.supervisor.ActorSupervisor
import com.akkamelo.core.supervisor.ActorSupervisor.PersistenceIdFactory
import com.typesafe.config.{Config, ConfigFactory}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.util.Success

object Boot extends App {
  private val config: Config = ConfigFactory.load()
  private val systemName: String = config.getString("boot.system.name")

  implicit val system: ActorSystem = ActorSystem(systemName, config)
  implicit val ec: ExecutionContext = system.dispatcher
  val materializer = Materializer(system)

  private val booter: Booter = Booter(system, ec, materializer, config)
  booter.greet()

  val clientActorSupervisor = booter.getClientActorSupervisor
  booter.registerInitialClients(clientActorSupervisor)
  val server: Option[Server] = booter.getServer(clientActorSupervisor)
  val persistenceTestServer: Option[PersistenceTestServer] = booter.getPersistenceTestServer

}

object Booter {
  def apply(system: ActorSystem, ec: ExecutionContext, materializer: Materializer, config: Config): Booter = new Booter(system, ec, materializer, config)
}

class Booter(val system: ActorSystem, val ec: ExecutionContext, val materializer: Materializer, val config: Config) extends BaseLogging {

  def getClientActorSupervisor: ActorRef = {
    val clientNamePrefix = config.getString("actor.client.name.prefix")
    val clientNameSuffix = config.getString("actor.client.name.suffix")
    val clientActorPassivationTimeout = Timeout(config.getLong("actor.client.passivationTimeoutSeconds"), TimeUnit.SECONDS).duration

    val clientActorPersistenceIdAndNameFactory: PersistenceIdFactory = (id: Int) => clientNamePrefix + id.toString + clientNameSuffix
    val clientPropsFactory = (persistenceId: String) => ClientActor.props(persistenceId, clientActorPassivationTimeout)

    system.actorOf(ActorSupervisor.props(clientPropsFactory, clientActorPersistenceIdAndNameFactory, clientActorPersistenceIdAndNameFactory), "client-actor-supervisor")
  }

  def registerInitialClients(clientSupervisor: ActorRef)(implicit ec: ExecutionContext): Unit = {
    val initialClients = config.getObjectList("boot.initialClients")
    implicit val clientRegisterTimeout: Timeout = Timeout(config.getLong("boot.initial-register.timeoutSeconds"), TimeUnit.SECONDS)
    logger.info(s"Registering initial clients if they dont exist")

    initialClients.forEach(client => {
      val id = client.get("id").unwrapped().asInstanceOf[Int]
      val initialBalance = client.get("initialBalance").unwrapped().asInstanceOf[Int]
      val limit = client.get("limit").unwrapped().asInstanceOf[Int]

      (clientSupervisor ? RegisterClient(id, limit, initialBalance)).mapTo[ClientActorResponse].onComplete({
        case Success(ClientRegistered(clientId)) => logger.info(s"Client $clientId registered.")
        case Success(ClientAlreadyExists) => logger.warn(s"Client $id already exists.")
        case any => logger.warn(s"Client $id could not be registered. Received: $any")
      })
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
    val clientActorRequestTimeout: Timeout = Timeout(config.getLong("actor.client.requestTimeoutSeconds"), TimeUnit.SECONDS)
    val host: String = config.getString("server.host")
    val port: Int = config.getInt("server.port")
    Server.newStartedAt(host, port, clientActorSupervisor)(system, ec, clientActorRequestTimeout)
  }

  def greet(): Unit = {
    val greeter = system.actorOf(GreeterActor.props, "greeter")
    greeter ! Configure(config.getString("boot.message"))
    greeter ! SayHello
  }

  def getPersistenceTestServer: Option[PersistenceTestServer] = {
    val persistenceTestServerEnabled = config.getBoolean("persistence-test-server.enabled")
    if (persistenceTestServerEnabled) {
      Some(startPersistenceTestServer)
    } else {
      None
    }
  }
  def startPersistenceTestServer: PersistenceTestServer = {
    val persistenceTestActor = system.actorOf(PersistentTestActor.props("persistence-test"), "persistence-test-actor")
    val persistenceTestHost: String = config.getString("persistence-test-server.host")
    val persistenceTestPort: Int = config.getInt("persistence-test-server.port")
    PersistenceTestServer(persistenceTestHost, persistenceTestPort, persistenceTestActor)(system, materializer, ec)
  }
}