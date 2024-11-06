package com.akkamelo.api.actor.client

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.akkamelo.api.actor.client.ClientActor.{ClientActorUnprocessableEntity, ClientAddTransactionCommand, ClientBalanceAndLimitResponse, ClientGetStatementCommand, ClientStatementResponse}
import com.akkamelo.api.actor.client.domain.state.{Client, Credit, Debit, TransactionType}
import com.akkamelo.api.actor.common.BaseActorSpec

class ClientActorSpec extends BaseActorSpec(ActorSystem("ClientActorSpec")) {

  import ClientActorSpec._

  "A client Actor" should "reply with the balance and limit after adding a transaction" in {
    val testProbe = TestProbe()
    val (clientState, clientActorRef) = resetClient(system, 1)

    val transactionValue = 100
    clientActorRef.tell(ClientAddTransactionCommand(transactionValue, TransactionType.CREDIT, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientBalanceAndLimitResponse(clientState.balance + transactionValue, clientState.limit))
  }

  it should "reply with the statement when requested" in {
    val testProbe = TestProbe()
    testProbe.ignoreMsg({
      case ClientBalanceAndLimitResponse(_, _) => true
      case _ => false
    })

    val (clientState, clientActorRef) = resetClient(system, 2)

    clientActorRef.tell(ClientAddTransactionCommand(100, TransactionType.CREDIT, "Test"), testProbe.ref)
    clientActorRef.tell(ClientAddTransactionCommand(50, TransactionType.DEBIT, "Test"), testProbe.ref)
    clientActorRef.tell(ClientAddTransactionCommand(25, TransactionType.CREDIT, "Test"), testProbe.ref)

    val updatedClientState = clientState.add(Credit(100, "Test"))
      .add(Debit(50, "Test"))
      .add(Credit(25, "Test"))

    clientActorRef.tell(ClientGetStatementCommand, testProbe.ref)
    testProbe.expectMsg(ClientStatementResponse(updatedClientState.getStatement))
  }

  it should "reply with an ActorProcessingFailure when the Transaction doesn't go through and ClientState should stay the same" in {
    val testProbe = TestProbe()
    val (clientState, clientActorRef) = resetClient(system, 3)

    clientActorRef.tell(ClientAddTransactionCommand(-100, TransactionType.CREDIT, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientActorUnprocessableEntity)

    clientActorRef.tell(ClientAddTransactionCommand(-100, TransactionType.DEBIT, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientActorUnprocessableEntity)

    clientActorRef.tell(ClientAddTransactionCommand(100, TransactionType.DEBIT, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientActorUnprocessableEntity)

    clientActorRef.tell(ClientAddTransactionCommand(100, TransactionType.NO_TYPE, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientActorUnprocessableEntity)

    clientActorRef.tell(ClientGetStatementCommand, testProbe.ref)
    testProbe.expectMsg(ClientStatementResponse(clientState.getStatement))
  }


}

object ClientActorSpec {
  val CLIENT_NAME_PREFIX = "client-"
  val CLIENT_NAME_SUFFIX = ""

  def resetClient(system: ActorSystem, clientId: Int, limit: Int = 0): (Client, ActorRef) = {
    val client = Client.initial.copy(id = clientId, limit = limit)
    val clientActorRef = system.actorOf(ClientActor.props(client), CLIENT_NAME_PREFIX + clientId + CLIENT_NAME_SUFFIX)
    (client, clientActorRef)
  }
}
