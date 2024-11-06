package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.ClientAddTransactionCommand
import com.akkamelo.api.actor.client.domain.state.{Client, Credit, Debit, TransactionType}
import com.akkamelo.api.actor.client.exception.ClientNotFoundException
import com.akkamelo.api.actor.client.handler.ClientAddTransactionHandler
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should
import org.scalatest.prop.TableDrivenPropertyChecks


class ClientAddTransactionHandlerSpec  extends AnyFlatSpecLike with TableDrivenPropertyChecks with should.Matchers {


  "TransactionHandle" should "apply transactions to Client state" in {

    val victim = ClientAddTransactionHandler.handle()
    val examples = Table(("description", "client", "transactionCommand", "expectation"),
        (
          "Initial Client 1",
          Client.initialWithId(1),
          ClientAddTransactionCommand(value = 100, transactionType = TransactionType.CREDIT, description = "descricao"),
          Client.initialWithId(1).copy(transactions = Client.initial.transactions :+ Credit(100,"descricao"))
        ),
      (
        "Initial Client 2",
        Client.initialWithId(2).copy(limit = 10000),
        ClientAddTransactionCommand(value = 100, transactionType = TransactionType.DEBIT, description = "descricao"),
        Client.initialWithId(2).copy(limit = 10000, transactions = Client.initial.transactions :+ Debit(100,"descricao"))
      )
    )
    forAll(examples) { (description, client, transactionCommand, expectation) =>
      victim(client,transactionCommand) should be(expectation)
    }

  }

  //Test if the ID is in the scope of this app. See: docs/api-contracts.md ## Initial Client Register
  "TransactionHandle" should "throws a exception if ID isn't in the app scope (1-5)" in {

    val victim = ClientAddTransactionHandler.handle()
    val examples = Table(("description", "client", "transactionCommand", "expectation"),
      (
        "Initial Client 1",
        Client.initial,
        ClientAddTransactionCommand(value = 100, transactionType = TransactionType.CREDIT, description = "descricao"),
        AnyRef
      )
      // Add more cases
    )
    forAll(examples) { (description, client, transactionCommand, expectation) =>
       assertThrows[ClientNotFoundException](victim(client,transactionCommand))
    }

  }

}
