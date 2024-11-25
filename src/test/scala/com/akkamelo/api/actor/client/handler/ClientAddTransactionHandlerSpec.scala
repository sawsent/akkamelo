package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.ClientAddTransactionCommand
import com.akkamelo.api.actor.client.domain.state.{Client, ClientState, Credit, Debit, TransactionType}
import com.akkamelo.api.actor.client.exception.{ClientNotFoundException, InvalidTransactionException}
import com.akkamelo.api.actor.client.handler.ClientAddTransactionHandler
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should
import org.scalatest.prop.TableDrivenPropertyChecks


class ClientAddTransactionHandlerSpec  extends AnyFlatSpecLike with TableDrivenPropertyChecks with should.Matchers {


  "TransactionHandle" should "apply transactions to Client state" in {

    val victim = ClientAddTransactionHandler().handle()
    val examples = Table(("description", "clientState", "transactionCommand", "expectation"),
        (
          "Initial Client 1",
          ClientState(Client.initialWithId(1)),
          ClientAddTransactionCommand(value = 100, transactionType = TransactionType.CREDIT, description = "descricao"),
          ClientState(Client.initialWithId(1).copy(transactions = Client.initial.transactions :+ Credit(100,"descricao")))
        ),
      (
        "Initial Client 2",
        ClientState(Client.initialWithId(2).copy(limit = 10000)),
        ClientAddTransactionCommand(value = 100, transactionType = TransactionType.DEBIT, description = "descricao"),
        ClientState(Client.initialWithId(2).copy(limit = 10000, transactions = Client.initial.transactions :+ Debit(100,"descricao")))
      )
    )
    forAll(examples) { (description, clientState, transactionCommand, expectation) =>
      victim(clientState,transactionCommand) should be(expectation)
    }

  }

  //Test if the ID is in the scope of this app. See: docs/api-contracts.md ## Initial Client Register
  it should "throws a exception if ID isn't in the app scope (1-5)" in {

    val victim = ClientAddTransactionHandler().handle()
    val examples = Table(("description", "clientState", "transactionCommand", "expectation"),
      (
        "Initial Client 0",
        ClientState(Client.initial),
        ClientAddTransactionCommand(value = 100, transactionType = TransactionType.CREDIT, description = "descricao"),
        AnyRef
      ),
      (
        "Initial Client 6",
        ClientState(Client.initial.copy(id = 6)),
        ClientAddTransactionCommand(value = 100, transactionType = TransactionType.CREDIT, description = "descricao"),
        AnyRef
      )
      // Add more cases
    )
    forAll(examples) { (description, client, transactionCommand, expectation) =>
       assertThrows[ClientNotFoundException](victim(client,transactionCommand))
    }

  }

  it should "throw InvalidTransactionException if transaction type is not specified" in {
    val victim = ClientAddTransactionHandler().handle()
    assertThrows[InvalidTransactionException](victim(ClientState(Client.initialWithId(1)), ClientAddTransactionCommand(100, TransactionType.NO_TYPE, "Test")))
  }

}
