package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.ClientAddTransactionCommand
import com.akkamelo.api.actor.client.domain.state.{Client, Credit, Debit, Transaction, TransactionType}
import com.akkamelo.api.actor.client.exception.ClientNotFoundException

object ClientAddTransactionHandler {
  type Handler = PartialFunction[(Client, ClientAddTransactionCommand), Client]

  def handle(): Handler = {
    case (client, ClientAddTransactionCommand(clientId, value, TransactionType.CREDIT, description)) =>
      validateClientId(clientId)
      addTransactionToClient(client, Credit(value, description))
    case (client, ClientAddTransactionCommand(clientId, value, TransactionType.DEBIT, description)) =>
      validateClientId(clientId)
      addTransactionToClient(client, Debit(value, description))
  }

  def addTransactionToClient(client: Client, transaction: Transaction): Client = client add transaction
  def validateClientId(id: Int): Unit = if (id < 1 || id > 5) throw ClientNotFoundException("Client is outside the App scope.")

}

