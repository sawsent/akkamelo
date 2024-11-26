package com.akkamelo.api.actor.client

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.api.actor.client.converter.ClientActorCommand2ActorEvent
import com.akkamelo.api.actor.client.domain.state._
import com.akkamelo.api.actor.client.handler.{ClientAddTransactionHandler, ClientAssignClientHandler}
import com.akkamelo.api.actor.common.BaseActorSpec
import org.mockito.MockitoSugar.{mock, when}

import scala.concurrent.duration.DurationInt

class ClientActorSpec extends BaseActorSpec(ActorSystem("ClientActorSpec")) {

  import ClientActorSpec._

  "A client Actor" should "reply with the balance and limit after adding a transaction" in {
    val clientId = 1

    val mockedTransactionAddCommand = ClientAddTransactionCommand(clientId, 100, TransactionType.CREDIT, "Test")
    val handlerMock = mock[ClientAddTransactionHandler]
    when(handlerMock.handle).thenReturn({
      case (_, command: ClientAddTransactionCommand) if command == mockedTransactionAddCommand =>
        ClientState(Client.initialWithId(clientId).copy(transactions = List(Credit(command.value, command.description))))
    })

    val testProbe = TestProbe()
    val (clientState, clientActorRef) = resetClient(system, clientId, addTransactionHandler = handlerMock)

    val transactionValue = 100
    clientActorRef.tell(ClientAddTransactionCommand(clientId, transactionValue, TransactionType.CREDIT, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientBalanceAndLimitResponse(clientState.client.balance + transactionValue, clientState.client.limit))
  }

  it should "reply with the statement when requested" in {
    val clientId = 2

    val mockedTransactionAddCommandCredit = ClientAddTransactionCommand(clientId, 100, TransactionType.CREDIT, "Test")
    val mockedTransactionAddCommandDebit = ClientAddTransactionCommand(clientId, 100, TransactionType.DEBIT, "Test")
    val handlerMock = mock[ClientAddTransactionHandler]
    when(handlerMock.handle).thenReturn({
      case (state: ClientState, command: ClientAddTransactionCommand) if command == mockedTransactionAddCommandCredit =>
        ClientState(state.client add Credit(command.value, command.description))
      case (state: ClientState, command: ClientAddTransactionCommand) if command == mockedTransactionAddCommandDebit =>
        ClientState(state.client add Debit(command.value, command.description))
    })

    val testProbe = TestProbe()
    testProbe.ignoreMsg({
      case ClientBalanceAndLimitResponse(_, _) => true
      case _ => false
    })

    val (clientState, clientActorRef) = resetClient(system, clientId, addTransactionHandler = handlerMock)

    clientActorRef.tell(mockedTransactionAddCommandCredit, testProbe.ref)
    clientActorRef.tell(mockedTransactionAddCommandDebit, testProbe.ref)
    clientActorRef.tell(mockedTransactionAddCommandCredit, testProbe.ref)

    val updatedClientState = handlerMock.handle(handlerMock.handle(handlerMock.handle(clientState, mockedTransactionAddCommandCredit),
      mockedTransactionAddCommandDebit), mockedTransactionAddCommandCredit).toFullState

    clientActorRef.tell(ClientGetStatementCommand, testProbe.ref)
    testProbe.expectMsg(ClientStatementResponse(updatedClientState.client.getStatement))
  }

  it should "reply with an ActorProcessingFailure when the Transaction doesn't go through and ClientState should stay the same" in {
    val testProbe = TestProbe()
    val clientId = 3
    val (clientState, clientActorRef) = resetClient(system, clientId)

    clientActorRef.tell(ClientAddTransactionCommand(clientId, -100, TransactionType.CREDIT, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientActorUnprocessableEntity)

    clientActorRef.tell(ClientAddTransactionCommand(clientId, -100, TransactionType.DEBIT, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientActorUnprocessableEntity)

    clientActorRef.tell(ClientAddTransactionCommand(clientId, 100, TransactionType.DEBIT, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientActorUnprocessableEntity)

    clientActorRef.tell(ClientAddTransactionCommand(clientId, 100, TransactionType.NO_TYPE, "Test"), testProbe.ref)
    testProbe.expectMsg(ClientActorUnprocessableEntity)

    clientActorRef.tell(ClientGetStatementCommand, testProbe.ref)
    testProbe.expectMsg(ClientStatementResponse(clientState.client.getStatement))
  }
}

object ClientActorSpec {
  val CLIENT_NAME_PREFIX = "client-"
  val CLIENT_NAME_SUFFIX = ""
  val clientActorPassivationTimeout = 1.minute

  def resetClient(system: ActorSystem,
                  clientId: Int,
                  limit: Int = 0,
                  addTransactionHandler: ClientAddTransactionHandler = ClientAddTransactionHandler(),
                  assignClientHandler: ClientAssignClientHandler = ClientAssignClientHandler(),
                  converter: ClientActorCommand2ActorEvent = ClientActorCommand2ActorEvent()): (ClientState, ActorRef) =
  {
    val client = Client.initial.copy(id = clientId, limit = limit)
    val clientActorNameAndPersistenceId = CLIENT_NAME_PREFIX + clientId + CLIENT_NAME_SUFFIX
    val clientActorRef = system.actorOf(ClientActor.props(CLIENT_NAME_PREFIX + clientId + CLIENT_NAME_SUFFIX,
      addTransactionHandler, assignClientHandler, converter, clientActorPassivationTimeout), clientActorNameAndPersistenceId)
    (ClientState(client), clientActorRef)
  }
}
