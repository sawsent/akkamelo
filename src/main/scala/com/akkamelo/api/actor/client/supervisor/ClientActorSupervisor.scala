package com.akkamelo.api.actor.client.supervisor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.akkamelo.api.actor.client.ClientActor
import com.akkamelo.api.actor.client.ClientActor.{ClientActorCommand, ClientActorResponse}
import com.akkamelo.api.actor.client.domain.state.Client

object ClientActorSupervisor {
  case class ResolveClientActor(clientId: Int) // => Option[ActorRef]
  case class ApplyCommand(clientId: Int, command: ClientActorCommand)

  case class NonExistingClientActor(clientId: Int) extends ClientActorResponse

  case class RegisterClientActor(clientId: Int) // => Either[RegistrationSuccess, RegistrationFailure]

  def props(getChildName: Int => String): Props = Props(new ClientActorSupervisor(getChildName))
}

class ClientActorSupervisor(val getChildName: Int => String) extends Actor with ActorLogging {
  import ClientActorSupervisor._

  override def receive: Receive = {
    case ApplyCommand(clientId, command) =>
      context.child(getChildName(clientId)) match {
        case Some(clientActor) =>
          clientActor.forward(command)
        case None =>
          sender() ! NonExistingClientActor(clientId)
          log.warning(s"Client Actor with id $clientId does not exist.")
      }
    case ResolveClientActor(clientId) =>
      sender() ! context.child(getChildName(clientId))
    case RegisterClientActor(clientId) =>
      registerClient(clientId)
  }

  private def registerClient(clientId: Int): Unit = {
    context.child(getChildName(clientId)) match {
      case Some(_) =>
        throw new IllegalArgumentException(s"Client with id $clientId already exists.")
      case None =>
        val client = Client.initialWithId(clientId)
        val clientActor = context.actorOf(ClientActor.props(client), getChildName(clientId))
        log.info(s"Client Actor with id $clientId registered.")
    }
  }
}
