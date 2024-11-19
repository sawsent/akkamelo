package com.akkamelo.api.actor.client

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.akkamelo.api.actor.client.converter.ClientActorCommand2ActorEvent
import com.akkamelo.api.actor.client.domain.state.{Client, ClientActorState, ClientNoState, ClientState, Statement, Transaction, TransactionType}
import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import com.akkamelo.api.actor.client.handler.{ClientAddTransactionHandler, ClientAssignClientHandler}

object ClientActor {

  // CommandRegion
  trait ClientActorCommand
  case class ClientAddTransactionCommand(value: Int, transactionType: TransactionType, description: String) extends ClientActorCommand
  case class AssignClientCommand(clientId: Int, initialLimit: Int, initialBalance: Int) extends ClientActorCommand
  case object ClientGetStatementCommand extends ClientActorCommand

  // EventRegion
  trait ClientActorEvent
  case class ClientTransactionAddedEvent(transaction: Transaction) extends ClientActorEvent
  case class ClientAssignedEvent(clientId: Int, initialLimit: Int, initialBalance: Int) extends ClientActorEvent

  // ResponseRegion
  trait ClientActorResponse
  case class ClientAssignedResponse(client: Client) extends ClientActorResponse
  case class ClientStatementResponse(statement: Statement) extends ClientActorResponse
  case class ClientBalanceAndLimitResponse(balance: Int, limit: Int) extends ClientActorResponse
  case object ClientActorUnprocessableEntity extends ClientActorResponse
  case class ClientActorPersistenceFailure(message: String) extends ClientActorResponse

  def props(persistenceId: String,
            addTransactionHandler: ClientAddTransactionHandler,
            clientAssignClientHandler: ClientAssignClientHandler,
            converter: ClientActorCommand2ActorEvent):
  Props = Props(new ClientActor(persistenceId, addTransactionHandler, clientAssignClientHandler, converter))

}

class ClientActor(persistenceIdentity: String,
                  val addTransactionHandler: ClientAddTransactionHandler,
                  val clientAssignClientHandler: ClientAssignClientHandler,
                  val converter: ClientActorCommand2ActorEvent) extends PersistentActor with ActorLogging
{
  import ClientActor._
  import context._

  override def persistenceId: String = persistenceIdentity

  override def receiveCommand: Receive = handleCommands(ClientNoState)

  def handleCommands(state: ClientActorState): Receive = {
    state match {
      case s: ClientState => handleClientCommands(s)
      case ClientNoState => handleNoStateCommands()
    }
  }

  def handleClientCommands(state: ClientState): Receive = {
    case cmd: ClientAddTransactionCommand =>
      log.info(s"Received a ClientAddTransactionCommand: $cmd")
      try {
        val updatedState = addTransactionHandler.handle()(state, cmd)
        sender() ! ClientBalanceAndLimitResponse(updatedState.client.balance, updatedState.client.limit)
        persist(converter.toActorEvent(cmd)) { evt =>
          log.info(s"Persisted TransactionAdded event: $evt")
          become(handleCommands(updatedState))
        }
      } catch {
        case e: InvalidTransactionException =>
          log.info(s"Transaction failure: ${e.getMessage}")
          sender() ! ClientActorUnprocessableEntity
        case e: Exception =>
          log.error(s"An error occurred: ${e.getMessage}")
          sender() ! ClientActorPersistenceFailure(e.getMessage)
      }

    case _: AssignClientCommand =>
      log.warning(s"Received an AssignClientCommand, when client is already assigned.")
      sender() ! ClientActorUnprocessableEntity

    case ClientGetStatementCommand =>
      log.info(s"Received a ClientGetStatementCommand")
      sender() ! ClientStatementResponse(state.client.getStatement)
  }

  def handleNoStateCommands(): Receive = {
    case cmd: AssignClientCommand =>
      log.info(s"Received an AssignClientCommand: $cmd")
      val state = clientAssignClientHandler.handle()(ClientNoState, cmd)
      try {
        persist(converter.toActorEvent(cmd)) { evt =>
          log.info(s"Persisted AssignClient event: $evt")
          become(handleCommands(state))
          sender() ! ClientAssignedResponse(state.client)
        }
      } catch {
        case e: Exception =>
          log.error(s"Persistence error occurred: ${e.getMessage}")
          sender() ! ClientActorPersistenceFailure(e.getMessage)
      }

    case any => log.warning(s"Received a message $any while without state. Ignoring.")
  }

  override def receiveRecover: Receive = recoverInitialState

  def recoverInitialState: Receive = {
    case ClientAssignedEvent(clientId, initialLimit, initialBalance) =>
      val state = ClientState(Client.initialWithId(clientId).copy(limit = initialLimit, balanceSnapshot = initialBalance))
      context.become(recoverWithState(state))
    case RecoveryCompleted =>
      log.info("Recovery completed.")
      context.become(handleCommands(ClientNoState))
  }

  def recoverWithState(state: ClientState): Receive = {
    case evt: ClientTransactionAddedEvent =>
      log.info(s"Recovering transaction: $evt")
      val updatedState = ClientState(state.client.add(evt.transaction))
      context.become(recoverWithState(ClientState(updatedState.client)))
    case RecoveryCompleted =>
      log.info("Recovery completed.")
      context.become(handleCommands(state))
  }

}
