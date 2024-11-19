package com.akkamelo.api.actor.client.converter

import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.api.actor.client.domain.state.{Credit, Debit, Transaction, TransactionType}

class ClientActorCommand2ActorEvent {
  def toActorEvent(actorCommand: ClientActorCommand): ClientActorEvent = actorCommand match {
    case ClientAddTransactionCommand(value, TransactionType.CREDIT, description) => ClientTransactionAddedEvent(Credit(value, description))
    case ClientAddTransactionCommand(value, TransactionType.DEBIT, description) => ClientTransactionAddedEvent(Debit(value, description))

    case AssignClientCommand(clientId, initialLimit, initialBalance) => ClientAssignedEvent(clientId, initialLimit, initialBalance)
  }
}

object ClientActorCommand2ActorEvent {
  def apply(): ClientActorCommand2ActorEvent = new ClientActorCommand2ActorEvent()
}
