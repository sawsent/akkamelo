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
import scala.util.Success

object ClientActorSupervisor {
  case class ApplyCommand(clientId: Int, command: ClientActorCommand)

  case class ClientActorRegistered(clientId: Int) extends ClientActorResponse
  case class NonExistingClientActor(clientId: Int) extends ClientActorResponse
  case class ClientActorAlreadyAssigned(clientId: Int) extends ClientActorResponse

  case class RegisterClientActor(clientId: Int, initialBalance: Int = 0, limit: Int = 0)

  def props(getChildName: Int => String, clientActorPassivationTimeout: FiniteDuration, clientActorRequestTimeout: Timeout): Props =
    Props(new ClientActorSupervisor(getChildName, clientActorPassivationTimeout, clientActorRequestTimeout))
}

class ClientActorSupervisor(val getChildName: Int => String, clientActorPassivationTimeout: FiniteDuration, clientActorRequestTimeout: Timeout) extends Actor with ActorLogging {
  import ClientActorSupervisor._
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val clientActorPassivationTimeoutDuration: FiniteDuration = clientActorPassivationTimeout
  implicit val clientActorRequestTimeoutDuration: Timeout = clientActorRequestTimeout

  override def receive: Receive = {
    case ApplyCommand(clientId, command) =>
      createOrRecoverClientActor(clientId).forward(command)

    case RegisterClientActor(clientId, initialBalance, limit) =>
      registerClient(clientId, initialBalance, limit, sender())
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

  private def registerClient(clientId: Int, initialBalance: Int, limit: Int, currentSender: ActorRef): Unit = {
    val clientActor = createOrRecoverClientActor(clientId)

    (clientActor ? ClientActorIsAssignedCommand).mapTo[ClientActorIsAssignedResponse].onComplete {
      case Success(ClientActorIsAssigned(id)) => currentSender ! ClientActorAlreadyAssigned(id)
      case Success(ClientActorIsNotAssigned) =>
        clientActor ! AssignClientCommand(clientId, limit, initialBalance)
        currentSender ! ClientActorRegistered(clientId)

      case _ => currentSender ! ClientActorUnprocessableEntity
    }

  }
}
