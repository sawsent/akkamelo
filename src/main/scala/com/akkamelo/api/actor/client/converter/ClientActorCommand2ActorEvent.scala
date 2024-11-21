package com.akkamelo.api.actor.client.converter

import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.api.actor.client.domain.state.{Credit, Debit, Transaction, TransactionType}

class ClientActorCommand2ActorEvent {
  def toActorEvent(actorCommand: ClientActorCommand): ClientActorEvent = actorCommand match {
    case cmd: ClientAddTransactionCommand if cmd.transactionType == TransactionType.CREDIT =>
      ClientTransactionAddedEvent(cmd.value, cmd.transactionType.toStringRepresentation, cmd.description)

    case RegisterClient(clientId, initialLimit, initialBalance) => ClientRegisteredEvent(clientId, initialLimit, initialBalance)
  }
}

object ClientActorCommand2ActorEvent {
  def apply(): ClientActorCommand2ActorEvent = new ClientActorCommand2ActorEvent()
}
