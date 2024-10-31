package com.akkamelo.api.actor.client.resolver

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.akkamelo.api.actor.client.ClientActor

object ClientActorResolver {
  case class ResolveClientActor(clientId: Int) // => Option[ActorRef]

  case class RegisterClientActor(clientId: Int) // => Either[RegistrationSuccess, RegistrationFailure]

  case object RegistrationSuccess

  case object RegistrationFailure

  def props: Props = Props(new ClientActorResolver)
}

class ClientActorResolver extends Actor with ActorLogging {
  import ClientActorResolver._

  override def receive: Receive = emptyState

  def emptyState: Receive = {
    case ResolveClientActor(clientId) =>
      sender() ! None
    case RegisterClientActor(clientId) =>
      val clientActor = context.actorOf(ClientActor.props(clientId), s"client-$clientId")
      context.become(registeredState(Map(clientId -> clientActor)))
      sender() ! RegistrationSuccess
  }

  def registeredState(intToRef: Map[Int, ActorRef]): Receive = {
    case ResolveClientActor(clientId) =>
      sender() ! intToRef.get(clientId)
    case RegisterClientActor(clientId) =>
      if (intToRef.contains(clientId)) {
        sender() ! RegistrationFailure
        throw new IllegalArgumentException(s"Client with id $clientId is already registered.")
      }
      val clientActor = context.actorOf(ClientActor.props(clientId), s"client-$clientId")
      context.become(registeredState(intToRef + (clientId -> clientActor)))
      sender() ! RegistrationSuccess
  }
}
