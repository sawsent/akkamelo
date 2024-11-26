package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.ClientAddTransactionCommand
import com.akkamelo.api.actor.client.domain.state._
import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should
import org.scalatest.prop.TableDrivenPropertyChecks


class ClientAddTransactionHandlerSpec extends AnyFlatSpecLike with TableDrivenPropertyChecks with should.Matchers {

  "TransactionHandle" should "apply transactions to Client state" in {

    val victim = ClientAddTransactionHandler().handle
    val examples = Table(("description", "clientState", "transactionCommand", "expectation"),
        (
          "Initial Client 1",
          ClientState(Client.initialWithId(1)),
          ClientAddTransactionCommand(entityId = 1, value = 100, transactionType = TransactionType.CREDIT, description = "descricao"),
          ClientState(Client.initialWithId(1).copy(transactions = Client.initial.transactions :+ Credit(100,"descricao")))
        ),
      (
        "Initial Client 2",
        ClientState(Client.initialWithId(2).copy(limit = 10000)),
        ClientAddTransactionCommand(entityId = 2, value = 100, transactionType = TransactionType.DEBIT, description = "descricao"),
        ClientState(Client.initialWithId(2).copy(limit = 10000, transactions = Client.initial.transactions :+ Debit(100,"descricao")))
      )
    )
    forAll(examples) { (description, clientState, transactionCommand, expectation) =>
      victim(clientState,transactionCommand) should be(expectation)
    }
  }

  it should "throw InvalidTransactionException if transaction type is not specified" in {
    val victim = ClientAddTransactionHandler().handle
    assertThrows[InvalidTransactionException](victim(ClientState(Client.initialWithId(1)), ClientAddTransactionCommand(1, 100, TransactionType.NO_TYPE, "Test")))
  }
}