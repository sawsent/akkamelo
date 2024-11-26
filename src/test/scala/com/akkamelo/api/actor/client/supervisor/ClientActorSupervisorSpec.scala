package com.akkamelo.api.actor.client.supervisor

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestActor.{AutoPilot, KeepRunning}
import akka.testkit.{EventFilter, TestProbe}
import com.akkamelo.api.actor.client.ClientActor.{ClientAlreadyExists, ClientDoesntExist, ClientGetStatementCommand, ClientRegistered, ClientStatementResponse, RegisterClient}
import com.akkamelo.api.actor.client.domain.state.Client
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor._
import com.akkamelo.api.actor.common.BaseActorSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class ClientActorSupervisorSpec extends BaseActorSpec(ActorSystem("ClientActorSupervisorSpec")) {
  import ClientActorSupervisorSpec._
  implicit val ec: ExecutionContext = system.dispatcher

  "A ClientActorSupervisor" should "register a client actor if it doesn't exist yet, and reply with ClientActorRegistered" in {
    val clientActorSupervisor = resetClientActorSupervisor(system, "cas1")
    val testProbe = TestProbe()
    testProbe.setAutoPilot(echoProbeAutoPilot)

    val clientId = 1
    val initialBalance = 100
    val limit = 50

    EventFilter.info(pattern = s"Client Actor $clientId registered. Initial balance: $initialBalance, limit: $limit") intercept {
      clientActorSupervisor.tell(RegisterClient(clientId, initialBalance, limit), testProbe.ref)
    }

    testProbe.expectMsg(ClientRegistered(clientId))
  }

  it should "log a warning if a client actor with the same ID already exists, and reply with ClientActorAlreadyExists" in {
    val clientActorSupervisor = resetClientActorSupervisor(system, "cas2")
    val testProbe = TestProbe()
    testProbe.setAutoPilot(echoProbeAutoPilot)

    val clientId = 1
    val initialBalance = 100
    val limit = 50

    clientActorSupervisor ! RegisterClient(clientId, initialBalance, limit)

    EventFilter.error(pattern = s"Client with id $clientId already exists.") intercept {
      clientActorSupervisor.tell(RegisterClient(clientId, initialBalance, limit), testProbe.ref)
    }
    testProbe.expectMsg(ClientAlreadyExists)
  }

  it should "forward a command to an existing client actor" in {

    val clientActorSupervisor = resetClientActorSupervisor(system, "cas3")
    val clientId = 1
    val testProbe = TestProbe()
    testProbe.setAutoPilot(echoProbeAutoPilot)
    testProbe.ignoreMsg({
      case ClientRegistered(_) => true
    })

    val command = ClientGetStatementCommand(clientId)

    // Create equal client actor to the one that will be registered by the supervisor, to get the expected response
    val expectedResponse: ClientStatementResponse = ClientStatementResponse(Client.initialWithId(clientId).getStatement)

    clientActorSupervisor.tell(ApplyCommand(clientId, command), testProbe.ref)

    testProbe.expectMsg(expectedResponse)
  }

  it should "reply with NonExistingClientActor if the client actor does not exist" in {
    val clientActorSupervisor = resetClientActorSupervisor(system, "cas4")
    val testProbe = TestProbe()
    testProbe.setAutoPilot(echoProbeAutoPilot)

    val clientId = 1
    val command = ClientGetStatementCommand(clientId)

    EventFilter.warning(pattern = s"Client Actor with id $clientId does not exist.") intercept {
      clientActorSupervisor.tell(ApplyCommand(clientId, command), testProbe.ref)
    }

    testProbe.expectMsg(ClientDoesntExist)
  }
}

object ClientActorSupervisorSpec {
  val CLIENT_ACTOR_NAME_PREFIX = "client-"
  val CLIENT_ACTOR_NAME_SUFFIX = ""
  val clientActorPassivationTimeout: FiniteDuration = 1.minute
  val clientActorRequestTimeout: FiniteDuration = 5.seconds

  def resetClientActorSupervisor(system: ActorSystem, name: String)(implicit ec: ExecutionContext): ActorRef = {
    system.actorOf(ClientActorSupervisor.props(_ => CLIENT_ACTOR_NAME_PREFIX + name + CLIENT_ACTOR_NAME_SUFFIX, clientActorPassivationTimeout, clientActorRequestTimeout), name)
  }

  val echoProbeAutoPilot: AutoPilot = (sender, msg) => {
      println(s"[TEST PROBE] Received message: $msg, from sender: $sender")
      KeepRunning
    }
}