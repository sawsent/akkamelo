package com.akkamelo.api.actor.client

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestProbe
import com.akkamelo.api.actor.client.ClientActor._
import com.akkamelo.api.actor.client.ClientActorSpec.{AddTransactionTestCommand, GetStatementTestCommand, RegisterClientTestCommand, TEST_ID, TestGetStateCommand, resetActorWithState, resetActorWithoutState, testActorPassivationTimeout}
import com.akkamelo.api.actor.client.converter.ClientActorCommand2ActorEvent
import com.akkamelo.api.actor.client.domain.state._
import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import com.akkamelo.api.actor.client.handler.{ClientAddTransactionHandler, ClientRegisterClientHandler}
import com.akkamelo.api.actor.common.BaseActorSpec
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.time.SpanSugar._

import scala.concurrent.duration.FiniteDuration

class ClientActorSpec extends BaseActorSpec(ActorSystem("ClientActorSpec")) {

  // TODO: mock handlers
  "A ClientActor with State" should "reply with the balance and limit after adding a transaction" in {
    val probe = TestProbe()

    val testClientMock = mock[Client]
    val actor: ActorRef = resetActorWithState("test-client-1", transactionHandlerReturn = () => ClientState(testClientMock), testClientMock)
    when(testClientMock.id).thenReturn(TEST_ID)
    when(testClientMock.balance).thenReturn(0)
    when(testClientMock.limit).thenReturn(0)

    probe.send(actor, AddTransactionTestCommand)
    probe.expectMsg(ClientBalanceAndLimitResponse(testClientMock.balance, testClientMock.limit))
  }

  it should "reply with the statement when requested" in {
    val probe = TestProbe()
    val testClientMock = mock[Client]
    val actor = resetActorWithState("test-client-2", () => ClientState(testClientMock), testClientMock)
    when(testClientMock.getStatement).thenReturn(mock[Statement])

    probe.send(actor, GetStatementTestCommand)
    probe.expectMsg(ClientStatementResponse(testClientMock.getStatement))

  }

  it should "reply with an ActorProcessingFailure when the Transaction doesn't go through and ClientState should stay the same" in {
    val probe = TestProbe()
    val testClientMock = mock[Client]
    val actor = resetActorWithState("test-client-3", () => throw InvalidTransactionException("test"), testClientMock)

    probe.send(actor, AddTransactionTestCommand)
    probe.expectMsg(ClientActorUnprocessableEntity)

    probe.send(actor, TestGetStateCommand)
    probe.expectMsg(ClientState(testClientMock))
  }

  it should "reply with ClientAlreadyExists when trying to register a client and the state shouldn't change" in {
    val probe = TestProbe()
    val testClientMock = mock[Client]
    val actor = resetActorWithState("test-client-4", () => throw new RuntimeException("Should not have used the handler"), testClientMock)

    probe.send(actor, RegisterClientTestCommand)
    probe.expectMsg(ClientAlreadyExists)

    probe.send(actor, TestGetStateCommand)
    probe.expectMsg(ClientState(testClientMock))
  }

  "A ClientActor without state" should "reply with an ActorNotFound when it receives a GetStatementCommand" in {
    val probe = TestProbe()
    val actor = resetActorWithoutState("test-client-5", () => ClientNoState, () => ClientNoState)

    probe.send(actor, GetStatementTestCommand)
    probe.expectMsg(ClientDoesntExist)

  }

  it should "reply with an ActorNotFound when it receives a AddTransactionCommand" in {
    val probe = TestProbe()
    val actor = resetActorWithoutState("test-client-6", () => ClientNoState, () => ClientNoState)

    probe.send(actor, AddTransactionTestCommand)
    probe.expectMsg(ClientDoesntExist)
  }
}

object ClientActorSpec {

  class TestClientActor(persistenceId: String,
                        passivationTimeout: FiniteDuration,
                        override val addTransactionHandler: ClientAddTransactionHandler,
                        override val clientAssignClientHandler: ClientRegisterClientHandler)
    extends ClientActor(persistenceId, addTransactionHandler, clientAssignClientHandler, ClientActorCommand2ActorEvent(), passivationTimeout) {

    override def unhandledCommand(state: ClientActorState): Receive = {
      case TestGetStateCommand => sender() ! state
    }
  }

  case object TestGetStateCommand

  case object TestClientState extends ClientActorState
  case class TestCommand(entityId: Int = 1) extends ClientActorCommand
  val TEST_ID = 1
  val testActorPassivationTimeout = 10.seconds
  val AddTransactionTestCommand: ClientAddTransactionCommand = ClientAddTransactionCommand(TEST_ID, 0, TransactionType.NO_TYPE, "")
  val RegisterClientTestCommand: RegisterClient = RegisterClient(TEST_ID, 0, 0)
  val GetStatementTestCommand: ClientGetStatementCommand = ClientGetStatementCommand(TEST_ID)

  def resetActorWithoutState(persistenceId: String,
                             transactionHandlerReturn: () => ClientActorState,
                             registerClientHandlerReturn: () => ClientActorState)(implicit system: ActorSystem): ActorRef = {

    val registerClientHandler: ClientRegisterClientHandler = mock[ClientRegisterClientHandler]
    when(registerClientHandler.handle).thenReturn({
      case (_, cmd) if cmd == RegisterClientTestCommand => registerClientHandlerReturn()
    })

    val transactionHandlerMock = mock[ClientAddTransactionHandler]
    when(transactionHandlerMock.handle).thenReturn({
      case (_, cmd) if cmd == AddTransactionTestCommand => transactionHandlerReturn()
    })

    system.actorOf(Props(new TestClientActor(persistenceId, testActorPassivationTimeout, transactionHandlerMock, registerClientHandler)))
  }
  def resetActorWithState(persistenceId: String, transactionHandlerReturn: () => ClientActorState, clientMock: Client)(implicit system: ActorSystem): ActorRef = {

    val registerClientHandler: ClientRegisterClientHandler = mock[ClientRegisterClientHandler]
    when(registerClientHandler.handle).thenReturn({
      case (_, cmd) if cmd == RegisterClientTestCommand => ClientState(clientMock)
    })

    val transactionHandlerMock = mock[ClientAddTransactionHandler]
    when(transactionHandlerMock.handle).thenReturn({
      case (_, cmd) if cmd == AddTransactionTestCommand => transactionHandlerReturn()
    })
    val actor: ActorRef = system.actorOf(Props(new TestClientActor(persistenceId, testActorPassivationTimeout, transactionHandlerMock, registerClientHandler)))
    val registerProbe = TestProbe()
    registerProbe.send(actor, RegisterClientTestCommand)
    actor
  }
}