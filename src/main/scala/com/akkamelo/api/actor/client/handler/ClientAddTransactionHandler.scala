package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.ClientAddTransactionCommand
import com.akkamelo.api.actor.client.domain.state.{Client, ClientActorState, ClientNoState, ClientState, Credit, Debit, Transaction, TransactionType}
import com.akkamelo.api.actor.client.exception.{ClientNotFoundException, InvalidTransactionException, TransactionConversionException}

class ClientAddTransactionHandler {
  import ClientAddTransactionHandler.Handler

  def handle(): Handler = {
    case (state: ClientState, ClientAddTransactionCommand(value, TransactionType.CREDIT, description)) =>
      validateClientId(state.client.id)
      val updatedClient = addTransactionToClient(state.client, Credit(value, description))
      ClientState(updatedClient)
    case (state: ClientState, ClientAddTransactionCommand(value, TransactionType.DEBIT, description)) =>
      validateClientId(state.client.id)
      val updatedClient = addTransactionToClient(state.client, Debit(value, description))
      ClientState(updatedClient)
    case (_, ClientAddTransactionCommand(_, TransactionType.NO_TYPE, _)) =>
      throw InvalidTransactionException("Transaction type must be specified.")
    case (ClientNoState, _) =>
      throw ClientNotFoundException("Tried to add transaction to non-existing client.")
  }

  def addTransactionToClient(client: Client, transaction: Transaction): Client = client add transaction

  def validateClientId(id: Int): Unit = if (id < 1 || id > 5) throw ClientNotFoundException("Client is outside the App scope.")

}

object ClientAddTransactionHandler {
  type Handler = PartialFunction[(ClientActorState, ClientAddTransactionCommand), ClientState]
  def apply(): ClientAddTransactionHandler = new ClientAddTransactionHandler()
}

