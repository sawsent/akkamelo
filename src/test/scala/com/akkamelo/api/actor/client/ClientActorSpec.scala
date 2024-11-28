
package com.akkamelo.api.actor.client

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.api.actor.client.domain.state.{ClientActorState, TransactionType}
import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import com.akkamelo.api.actor.client.handler.ClientAddTransactionHandler
import com.akkamelo.api.actor.common.BaseActorSpec
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar._
import org.scalatest.wordspec.AnyWordSpecLike

class ClientActorSpec extends BaseActorSpec(ActorSystem("ClientActorSpec")) {

  // TODO: mock handlers
  "A ClientActor" should "reply with the balance and limit after adding a transaction" in {
    val probe = TestProbe()
    val actor = system.actorOf(ClientActor.props("test-client-1", 10.seconds))

    val initialLimit = 5000
    val initialBalance = 2000
    probe.send(actor, RegisterClient(1, initialLimit, initialBalance))
    probe.expectMsg(ClientRegistered(1))

    val transactionValue = 500
    probe.send(actor, ClientAddTransactionCommand(1, transactionValue, TransactionType.DEBIT, "Purchase"))
    probe.expectMsg(ClientBalanceAndLimitResponse(initialBalance - transactionValue, initialLimit))

    probe.send(actor, ClientGetStatementCommand(1))
    val response = probe.expectMsgType[ClientStatementResponse]
    assert(response.statement.lastTransactions.size == 1)
  }

  it should "reply with the statement when requested" in {
    val probe = TestProbe()
    val actor = system.actorOf(ClientActor.props("test-client-2", 10.seconds))

    val initialLimit = 3000
    val initialBalance = 1000
    probe.send(actor, RegisterClient(2, initialLimit, initialBalance))
    probe.expectMsg(ClientRegistered(2))

    probe.send(actor, ClientAddTransactionCommand(2, 200, TransactionType.CREDIT, "Refund"))
    probe.expectMsgType[ClientBalanceAndLimitResponse]
    probe.send(actor, ClientAddTransactionCommand(2, 50, TransactionType.DEBIT, "Snack"))
    probe.expectMsgType[ClientBalanceAndLimitResponse]

    probe.send(actor, ClientGetStatementCommand(2))
    val response = probe.expectMsgType[ClientStatementResponse]
    assert(response.statement.lastTransactions.size == 2)
    assert(response.statement.lastTransactions.exists(_.description == "Refund"))
    assert(response.statement.lastTransactions.exists(_.description == "Snack"))
  }

  it should "reply with an ActorProcessingFailure when the Transaction doesn't go through and ClientState should stay the same" in {
    val probe = TestProbe()
    val mockedTransactionHandler = mock[ClientAddTransactionHandler]
    when(mockedTransactionHandler.handle).thenReturn({
      case (_, _) => throw InvalidTransactionException("Transaction failed")
    })

    val actor = system.actorOf(ClientActor.props("test-client-3", 10.seconds, addTransactionHandler = mockedTransactionHandler))

    val initialLimit = 1000
    val initialBalance = 100
    probe.send(actor, RegisterClient(3, initialLimit, initialBalance))
    probe.expectMsg(ClientRegistered(3))

    probe.send(actor, ClientAddTransactionCommand(3, 200, TransactionType.DEBIT, "Overdraft"))
    probe.expectMsg(ClientActorUnprocessableEntity)

    probe.send(actor, ClientGetStatementCommand(3))
    val response = probe.expectMsgType[ClientStatementResponse]
    assert(response.statement.lastTransactions.isEmpty)
  }

  it should "reply with an ActorNotFound when the ClientState doesn't exist" in {
    val probe = TestProbe()
    val actor = system.actorOf(ClientActor.props("test-client-4", 10.seconds))

    probe.send(actor, ClientGetStatementCommand(4))
    probe.expectMsg(ClientDoesntExist)

    probe.send(actor, ClientAddTransactionCommand(4, 100, TransactionType.CREDIT, "Bonus"))
    probe.expectMsg(ClientDoesntExist)
  }

  it should "reply with an ActorAlreadyExists when the ClientState already exists" in {
    val probe = TestProbe()
    val actor = system.actorOf(ClientActor.props("test-client-5", 10.seconds))

    probe.send(actor, RegisterClient(5, 2000, 500))
    probe.expectMsg(ClientRegistered(5))

    probe.send(actor, RegisterClient(5, 1000, 100))
    probe.expectMsg(ClientAlreadyExists)
  }
}
