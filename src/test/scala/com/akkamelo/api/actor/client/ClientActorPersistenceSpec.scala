package com.akkamelo.api.actor.client

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.api.actor.client.ClientActorPersistenceSpec.getPersistenceIdForTest
import com.akkamelo.api.actor.client.domain.state.TransactionType
import com.akkamelo.api.actor.common.BaseActorSpec
import org.scalatest.time.SpanSugar._

class ClientActorPersistenceSpec extends BaseActorSpec(ActorSystem("ClientActorPersistenceSpec")) {

  val testActorPassivationTimeout = 10.seconds

  "A ClientActor" should "persist and recover state correctly for client registration" in {
    val probe = TestProbe()

    val persistenceId = getPersistenceIdForTest

    val actor: ActorRef = system.actorOf(ClientActor.props(persistenceId, testActorPassivationTimeout))
    val clientId = 1
    val initialLimit = 5000
    val initialBalance = 2000

    probe.send(actor, RegisterClient(clientId, initialLimit, initialBalance))
    probe.expectMsg(ClientRegistered(clientId))

    system.stop(actor)

    val recoveredActor = system.actorOf(ClientActor.props(persistenceId, testActorPassivationTimeout))
    probe.send(recoveredActor, ClientGetStatementCommand(clientId))
    val response = probe.expectMsgType[ClientStatementResponse]

    assert(response.statement.balanceInformation.balance == initialBalance)
    assert(response.statement.lastTransactions.isEmpty)
  }

  it should "persist and recover state correctly for adding a transaction" in {
    val probe = TestProbe()
    val persistenceId = getPersistenceIdForTest

    val actor: ActorRef = system.actorOf(ClientActor.props(persistenceId, testActorPassivationTimeout))
    val clientId = 2
    val initialLimit = 3000
    val initialBalance = 1000
    probe.send(actor, RegisterClient(clientId, initialLimit, initialBalance))
    probe.expectMsg(ClientRegistered(clientId))

    val transactionValue = 200
    probe.send(actor, ClientAddTransactionCommand(clientId, transactionValue, TransactionType.CREDIT, "Refund"))
    probe.expectMsgType[ClientBalanceAndLimitResponse]

    system.stop(actor)

    val recoveredActor = system.actorOf(ClientActor.props(persistenceId, testActorPassivationTimeout))
    probe.send(recoveredActor, ClientGetStatementCommand(clientId))
    val response = probe.expectMsgType[ClientStatementResponse]

    assert(response.statement.lastTransactions.size == 1)
    assert(response.statement.lastTransactions.head.description == "Refund")
    assert(response.statement.lastTransactions.head.value == transactionValue)
  }

  it should "not recover any state if no events are persisted" in {
    val probe = TestProbe()
    val persistenceId = getPersistenceIdForTest

    val actor: ActorRef = system.actorOf(ClientActor.props(persistenceId, testActorPassivationTimeout))

    probe.send(actor, ClientGetStatementCommand(999))
    probe.expectMsg(ClientDoesntExist)
  }

}

object ClientActorPersistenceSpec {
  def getPersistenceIdForTest: String = s"test-client-persistence-${System.currentTimeMillis()}"
}
