package com.akkamelo.api.actor.client.converter

import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.core.actor.converter.ActorCommand2ActorEvent

class ClientActorCommand2ActorEvent extends ActorCommand2ActorEvent[ClientActorCommand, ClientActorEvent] {

  override def toActorEvent(actorCommand: ClientActorCommand): ClientActorEvent = actorCommand match {
    case cmd: ClientAddTransactionCommand =>
      ClientTransactionAddedEvent(cmd.entityId, cmd.value, cmd.transactionType.toStringRepresentation, cmd.description)

    case RegisterClient(clientId, initialLimit, initialBalance) => ClientRegisteredEvent(clientId, initialLimit, initialBalance)
  }
}

object ClientActorCommand2ActorEvent {
  def apply(): ClientActorCommand2ActorEvent = new ClientActorCommand2ActorEvent()
}
