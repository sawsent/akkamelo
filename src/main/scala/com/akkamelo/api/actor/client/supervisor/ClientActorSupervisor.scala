package com.akkamelo.api.actor.client.supervisor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.akkamelo.api.actor.client.ClientActor
import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.api.actor.client.converter.ClientActorCommand2ActorEvent
import com.akkamelo.api.actor.client.handler.{ClientAddTransactionHandler, ClientAssignClientHandler}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object ClientActorSupervisor {
  case class ApplyCommand(clientId: Int, command: ClientActorCommand)


  def props(getChildName: Int => String, clientActorPassivationTimeout: FiniteDuration, clientActorRequestTimeout: Timeout): Props =
    Props(new ClientActorSupervisor(getChildName, clientActorPassivationTimeout, clientActorRequestTimeout))
}

class ClientActorSupervisor(val getChildName: Int => String, clientActorPassivationTimeout: FiniteDuration, clientActorRequestTimeout: Timeout) extends Actor with ActorLogging {
  import ClientActorSupervisor._
  implicit val clientActorPassivationTimeoutDuration: FiniteDuration = clientActorPassivationTimeout
  implicit val clientActorRequestTimeoutDuration: Timeout = clientActorRequestTimeout

  override def receive: Receive = {
    case ApplyCommand(clientId, command) =>
      createOrRecoverClientActor(clientId).forward(command)
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
}
