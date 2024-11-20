package com.akkamelo.api.actor.client.supervisor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.akkamelo.api.actor.client.ClientActor
import com.akkamelo.api.actor.client.ClientActor.{AssignClientCommand, ClientActorCommand, ClientActorResponse}
import com.akkamelo.api.actor.client.converter.ClientActorCommand2ActorEvent
import com.akkamelo.api.actor.client.domain.state.Client
import com.akkamelo.api.actor.client.handler.{ClientAddTransactionHandler, ClientAssignClientHandler}

import scala.concurrent.duration.FiniteDuration

object ClientActorSupervisor {
  case class ApplyCommand(clientId: Int, command: ClientActorCommand)

  case class ClientActorRegistered(clientId: Int) extends ClientActorResponse
  case class NonExistingClientActor(clientId: Int) extends ClientActorResponse
  case class ClientActorAlreadyExists(clientId: Int) extends ClientActorResponse

  case class RegisterClientActor(clientId: Int, initialBalance: Int = 0, limit: Int = 0)

  def props(getChildName: Int => String, clientActorPassivationTimeout: FiniteDuration): Props =
    Props(new ClientActorSupervisor(getChildName, clientActorPassivationTimeout))
}

class ClientActorSupervisor(val getChildName: Int => String, val clientActorPassivationTimeout: FiniteDuration) extends Actor with ActorLogging {
  import ClientActorSupervisor._

  override def receive: Receive = {
    case ApplyCommand(clientId, command) =>
      context.child(getChildName(clientId)) match {
        case Some(clientActor) =>
          clientActor.forward(command)
        case None =>
          val clientActor = createOrRecoverClientActor(clientId)
          clientActor.forward(command)
      }
    case RegisterClientActor(clientId, initialBalance, limit) =>
      registerClient(clientId, initialBalance, limit)
  }

  private def createOrRecoverClientActor(clientId: Int): ActorRef = {
    context.child(getChildName(clientId)) match {
      case Some(clientActor) => clientActor
      case None =>
        val clientActor = context.actorOf(ClientActor.props(getChildName(clientId),
          ClientAddTransactionHandler(),
          ClientAssignClientHandler(),
          ClientActorCommand2ActorEvent(),
          clientActorPassivationTimeout), getChildName(clientId))
        clientActor
    }
  }

  private def registerClient(clientId: Int, initialBalance: Int, limit: Int): Unit = {
    context.child(getChildName(clientId)) match {
      case Some(_) =>
        log.warning(s"Client with id $clientId already exists.")
        sender() ! ClientActorAlreadyExists(clientId)
      case None =>
        val client = Client.initial.copy(id = clientId, balanceSnapshot = initialBalance, limit = limit)
        val clientActor = createOrRecoverClientActor(clientId)
        clientActor ! AssignClientCommand(client.id, client.limit, client.balanceSnapshot)
        log.info(s"Client Actor $clientId registered. Initial balance: $initialBalance, limit: $limit")
        sender() ! ClientActorRegistered(clientId)
    }
  }
}
