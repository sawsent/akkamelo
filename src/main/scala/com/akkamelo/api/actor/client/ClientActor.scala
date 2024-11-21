package com.akkamelo.api.actor.client

import akka.actor.{ActorLogging, Props, ReceiveTimeout}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.akkamelo.api.actor.client.converter.ClientActorCommand2ActorEvent
import com.akkamelo.api.actor.client.domain.state._
import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import com.akkamelo.api.actor.client.handler.{ClientAddTransactionHandler, ClientAssignClientHandler}

import scala.concurrent.duration.FiniteDuration

object ClientActor {

  // CommandRegion
  trait ClientActorCommand
  case class ClientAddTransactionCommand(value: Int, transactionType: TransactionType, description: String) extends ClientActorCommand
  case class RegisterClient(clientId: Int, initialLimit: Int, initialBalance: Int) extends ClientActorCommand
  case object ClientGetStatementCommand extends ClientActorCommand

  // EventRegion
  trait ClientActorEvent
  case class ClientTransactionAddedEvent(value: Int, transactionType: String, description: String) extends ClientActorEvent
  case class ClientRegisteredEvent(clientId: Int, initialLimit: Int, initialBalance: Int) extends ClientActorEvent

  // ResponseRegion
  sealed trait ClientActorResponse extends Serializable
  case class ClientStatementResponse(statement: Statement) extends ClientActorResponse
  case class ClientBalanceAndLimitResponse(balance: Int, limit: Int) extends ClientActorResponse
  case object ClientActorUnprocessableEntity extends ClientActorResponse
  case class ClientActorPersistenceFailure(message: String) extends ClientActorResponse
  case class ClientRegistered(clientId: Int) extends ClientActorResponse

  case object ClientDoesntExist extends ClientActorResponse
  case object ClientAlreadyExists extends ClientActorResponse

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
                  val passivationTimeout: FiniteDuration) extends PersistentActor with ActorLogging
{
  import ClientActor._
  import context._

  override def persistenceId: String = persistenceIdentity
  setReceiveTimeout(passivationTimeout)

  override def receiveCommand: Receive = receive(ClientNoState)

  private def passivationStrategy: Receive = {
    case ReceiveTimeout =>
      log.info(s"It's been $passivationTimeout since the last message. Passivating self.")
      stop(self)
  }

  private def unhandledCommand(state: ClientActorState): Receive = {
    case any =>
      log.warning(s"Received an unhandled message: $any, with state: $state")
      sender() ! ClientActorUnprocessableEntity
  }

  private def receive(state: ClientActorState): Receive = passivationStrategy.orElse(handleCommands(state)).orElse(unhandledCommand(state))

  private def handleCommands(state: ClientActorState): Receive = {
    state match {
      case s: ClientState => handleClientCommands(s)
      case ClientNoState => handleNoStateCommands
    }
  }

  private def handleClientCommands(state: ClientState): Receive = {
    case cmd: ClientAddTransactionCommand =>
      log.info(s"Received a ClientAddTransactionCommand: $cmd")
      try {
        val updatedState = addTransactionHandler.handle()(state, cmd)
        val event = converter.toActorEvent(cmd)
        doPersist(event)(changeStateAndRespond(updatedState)(ClientBalanceAndLimitResponse(updatedState.client.balance, updatedState.client.limit)))
      } catch {
        case e: InvalidTransactionException =>
          log.info(s"Transaction failure: ${e.getMessage}")
          sender() ! ClientActorUnprocessableEntity
      }

    case ClientGetStatementCommand =>
      log.info(s"Received a ClientGetStatementCommand")
      sender() ! ClientStatementResponse(state.client.getStatement)

    case RegisterClient(_, _, _) =>
      log.info(s"Received a RegisterClient while already registered. Ignoring.")
      sender() ! ClientAlreadyExists
  }

  private def handleNoStateCommands: Receive = {
    case cmd: RegisterClient =>
      log.info(s"Received an AssignClientCommand: $cmd")
      val state = clientAssignClientHandler.handle()(ClientNoState, cmd)
      val event = converter.toActorEvent(cmd)
      doPersist(event)(changeState(state))

    case _: ClientActorCommand =>
      log.warning(s"Received a command before being registered.")
      sender() ! ClientDoesntExist
  }

  private def doPersist(evt: ClientActorEvent)(doAfter: () => Unit): Unit = {
    persist(evt) { _ =>
      log.info(s"Persisted $evt")
      doAfter()
    }
  }

  private def changeState(newState: ClientActorState): () => Unit = () => become(receive(newState))
  private def changeStateAndRespond(newState: ClientActorState)(response: ClientActorResponse): () => Unit = () => {
    become(receive(newState))
    sender() ! response
  }


  override def receiveRecover: Receive = {
    var state: ClientActorState = ClientNoState

    val receiveRecoverBehaviour: Receive = {
      case RecoveryCompleted =>
        log.info(s"Recovery completed. Final state: $state")
        become(receive(state))

      case evt: ClientActorEvent =>
        state match {
          case ClientNoState => state = recoverInitialState(evt)
          case s: ClientState => state = recoverWithState(s, evt)
        }
        log.info(s"Recovered $evt, new state: $state")

    }
    receiveRecoverBehaviour
  }

  private def recoverInitialState(evt: ClientActorEvent): ClientActorState = evt match {
    case ClientRegisteredEvent(clientId, initialLimit, initialBalance) =>
      val state = ClientState(Client.initialWithId(clientId).copy(limit = initialLimit, balanceSnapshot = initialBalance))
      state
  }

  private def recoverWithState(state: ClientState, evt: ClientActorEvent): ClientActorState = evt match {
    case evt: ClientTransactionAddedEvent =>
      val updatedState = TransactionType.fromStringRepresentation(evt.transactionType) match {
        case TransactionType.CREDIT => ClientState(state.client.add(Credit(evt.value, evt.description)))
        case TransactionType.DEBIT => ClientState(state.client.add(Debit(evt.value, evt.description)))
      }
      updatedState
  }
}
