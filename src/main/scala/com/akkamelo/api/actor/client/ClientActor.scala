package com.akkamelo.api.actor.client

import akka.actor.{Actor, ActorLogging, Props, ReceiveTimeout}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.akkamelo.api.actor.client.converter.ClientActorCommand2ActorEvent
import com.akkamelo.api.actor.client.domain.state.{Client, ClientActorState, ClientNoState, ClientState, Credit, Debit, Statement, Transaction, TransactionType}
import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import com.akkamelo.api.actor.client.handler.{ClientAddTransactionHandler, ClientAssignClientHandler}
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor.NonExistingClientActor

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ClientActor {

  // CommandRegion
  trait ClientActorCommand
  case class ClientAddTransactionCommand(value: Int, transactionType: TransactionType, description: String) extends ClientActorCommand
  case class AssignClientCommand(clientId: Int, initialLimit: Int, initialBalance: Int) extends ClientActorCommand
  case object ClientGetStatementCommand extends ClientActorCommand

  // EventRegion
  trait ClientActorEvent
  case class ClientTransactionAddedEvent(value: Int, transactionType: String, description: String) extends ClientActorEvent
  case class ClientAssignedEvent(clientId: Int, initialLimit: Int, initialBalance: Int) extends ClientActorEvent

  // ResponseRegion
  trait ClientActorResponse
  case class ClientAssignedResponse(client: Client) extends ClientActorResponse
  case object ClientActorNotAssigned extends ClientActorResponse
  case class ClientStatementResponse(statement: Statement) extends ClientActorResponse
  case class ClientBalanceAndLimitResponse(balance: Int, limit: Int) extends ClientActorResponse
  case object ClientActorUnprocessableEntity extends ClientActorResponse
  case class ClientActorPersistenceFailure(message: String) extends ClientActorResponse

  def props(persistenceId: String,
            addTransactionHandler: ClientAddTransactionHandler,
            clientAssignClientHandler: ClientAssignClientHandler,
            converter: ClientActorCommand2ActorEvent,
            passivationTimeout: FiniteDuration):
  Props = Props(new ClientActor(persistenceId, addTransactionHandler, clientAssignClientHandler, converter, passivationTimeout))

}

class ClientActor(persistenceIdentity: String,
                  val addTransactionHandler: ClientAddTransactionHandler,
                  val clientAssignClientHandler: ClientAssignClientHandler,
                  val converter: ClientActorCommand2ActorEvent,
                  passivationTimeout: FiniteDuration) extends PersistentActor with ActorLogging
{
  import ClientActor._
  import context._

  override def persistenceId: String = persistenceIdentity
  setReceiveTimeout(passivationTimeout)

  override def receiveCommand: Receive = handleCommands(ClientNoState)

  def passivationStrategy: Receive = {
    case ReceiveTimeout =>
      log.info(s"Passivating actor with id $persistenceId.")
      context.stop(self)
  }

  def handleCommands(state: ClientActorState): Receive = passivationStrategy.orElse({
    state match {
      case s: ClientState => handleClientCommands(s)
      case ClientNoState => handleNoStateCommands()
    }
  })

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

    case any: ClientActorCommand =>
      log.warning(s"Received a message $any while without state. Ignoring.")
      sender() ! NonExistingClientActor

  }

  override def receiveRecover: Receive = {
    var state: ClientActorState = ClientNoState

    val behaviour: Receive = {
      case RecoveryCompleted =>
        log.info(s"Recovery completed. Final state: $state")
        context.become(handleCommands(state))

      case evt: ClientAssignedEvent => state match {
        case ClientNoState =>
          state = recoverInitialState(evt)
        case _ =>
          log.warning(s"Received a ClientAssignedEvent while already assigned. Ignoring.")
      }

      case evt: ClientTransactionAddedEvent => state match {
          case s: ClientState =>
            val updatedState = recoverWithState(s, evt)
            state = updatedState
          case _ =>
            log.warning(s"Received a ClientTransactionAddedEvent while without state. Ignoring.")
      }
    }
    behaviour
  }

  def recoverInitialState(evt: ClientActorEvent): ClientActorState = evt match {
    case ClientAssignedEvent(clientId, initialLimit, initialBalance) =>
      val state = ClientState(Client.initialWithId(clientId).copy(limit = initialLimit, balanceSnapshot = initialBalance))
      log.info(s"Recovered clientAssignedEvent, new state: $state")
      state
  }

  def recoverWithState(state: ClientState, evt: ClientActorEvent): ClientActorState = evt match {
    case evt: ClientTransactionAddedEvent =>
      log.info(s"Recovering transaction: $evt")
      val updatedState = TransactionType.fromStringRepresentation(evt.transactionType) match {
        case TransactionType.CREDIT => ClientState(state.client.add(Credit(evt.value, evt.description)))
        case TransactionType.DEBIT => ClientState(state.client.add(Debit(evt.value, evt.description)))
      }
      log.info(s"Recovered clientAssignedEvent, new state: $updatedState")
      updatedState
  }
}
