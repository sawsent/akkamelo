package com.akkamelo.api.actor.client

import akka.actor.{Actor, ActorLogging, Props}
import com.akkamelo.api.actor.client.domain.state.{Client, ClientActorState, ClientNoState, ClientState, Statement, TransactionType}
import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import com.akkamelo.api.actor.client.handler.ClientAddTransactionHandler

object ClientActor {

  // CommandRegion
  trait ClientActorCommand
  case class ClientAddTransactionCommand(value: Int, transactionType: TransactionType, description: String) extends ClientActorCommand
  case object ClientGetStatementCommand extends ClientActorCommand

  // EventRegion
  trait ClientActorEvent
  case class ClientTransactionAddedEvent(clientId: Int, value: Int, transactionType: TransactionType, description: String) extends ClientActorEvent

  // ResponseRegion
  trait ClientActorResponse
  case class ClientStatementResponse(statement: Statement) extends ClientActorResponse
  case class ClientBalanceAndLimitResponse(balance: Int, limit: Int) extends ClientActorResponse
  case object ClientActorProcessingFailure extends ClientActorResponse

  def props(client: Client): Props = Props(new ClientActor(client))

}

class ClientActor(val client: Client) extends Actor with ActorLogging {
  import ClientActor._
  import context._

  override def receive: Receive = handleCommands(ClientState(client))
    .orElse {
    case _ => log.warning(s"Received a message that it cannot handle.")
  }

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
        val updatedClient = ClientAddTransactionHandler.handle()(state.client, cmd)
        sender() ! ClientBalanceAndLimitResponse(updatedClient.balance, updatedClient.limit)
        become(handleCommands(ClientState(updatedClient)))
      } catch {
        case e: InvalidTransactionException =>
          log.info(s"Transaction failure: ${e.getMessage}. Replying with ClientActorProcessingFailure.")
          sender() ! ClientActorProcessingFailure
      }

    case ClientGetStatementCommand =>
      log.info(s"Received a ClientGetStatementCommand")
      val client = state.client
      val statement = client.getStatement
      sender() ! ClientStatementResponse(statement)
  }

  def handleNoStateCommands(): Receive = {
    case _ => log.warning(s"Received a message while without state. Ignoring.")
  }

}
