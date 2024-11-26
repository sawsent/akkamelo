package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.ClientAddTransactionCommand
import com.akkamelo.api.actor.client.domain.state._
import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import com.akkamelo.core.actor.handler.CommandHandler

class ClientAddTransactionHandler extends CommandHandler[ClientActorState, ClientAddTransactionCommand] {
  override def handle: Handler = {
    case (state: ClientState, ClientAddTransactionCommand(_, value, TransactionType.CREDIT, description)) =>
      val updatedClient = state.client add Credit(value, description)
      ClientState(updatedClient)
    case (state: ClientState, ClientAddTransactionCommand(_, value, TransactionType.DEBIT, description)) =>
      val updatedClient = state.client add Debit(value, description)
      ClientState(updatedClient)
    case (_, ClientAddTransactionCommand(_, _, TransactionType.NO_TYPE, _)) =>
      throw InvalidTransactionException("Transaction type must be specified.")
    case (ClientNoState, _) =>
      throw InvalidTransactionException("Tried to add transaction to non-existing client.")
  }
}

object ClientAddTransactionHandler {
  def apply(): ClientAddTransactionHandler = new ClientAddTransactionHandler()
}