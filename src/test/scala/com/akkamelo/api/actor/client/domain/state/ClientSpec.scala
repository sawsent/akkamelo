package com.akkamelo.api.actor.client.domain.state

import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import org.scalatest.flatspec.AnyFlatSpec

class ClientSpec extends AnyFlatSpec {

  "A Client" should "a empty Transactions List at initial state" in {
    assert(Client.initial.transactions.isEmpty)
  }

  it should "return new client state when a Credit is added" in {
    val transaction =  Credit(100, "desc")
    val client = Client.initial
    val resultedClient = client add transaction
    assert(resultedClient.transactions.head.value == 100)
  }

  it should "Throw an exception if add a Debit that will result a balance lower than Client Limit" in {
    val transaction =  Debit(100001, "desc")
    val client = Client.initial.copy(limit=100000)
    assertThrows[InvalidTransactionException] {
      client add transaction
    }
  }

  it should "use the transaction list to return the statement (with only the last 10 transactions) and the balance" in {
    val creditTransaction = Credit(100,"desc")
    val debitTransaction = Debit(500,"desc")
    val clientState = Client.initial.copy(id = 1, limit =100000)
    val resultClientState = (clientState add creditTransaction)
      .add (debitTransaction)
      .add (creditTransaction)
      .add (creditTransaction)
      .add (creditTransaction)
      .add (creditTransaction)
      .add (debitTransaction)
      .add (debitTransaction)
      .add (creditTransaction)
      .add (creditTransaction)
      .add (creditTransaction)
      .add (creditTransaction)
    val statement = resultClientState.getStatement
    assert(statement.balanceInformation.balance == -600)
    assert(statement.lastTransactions.size == 10)
  }
}