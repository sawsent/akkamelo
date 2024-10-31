package com.akkamelo.api.actor.client

import akka.actor.{Actor, ActorLogging, Props}
import com.akkamelo.api.actor.client.domain.state.{Client, ClientActorState, ClientNoState, ClientState, TransactionType}
import com.akkamelo.api.actor.client.handler.ClientAddTransactionHandler

object ClientActor {

  // CommandRegion
  trait ClientActorCommand
  case class ClientAddTransactionCommand(clientId: Int, value: Int, transactionType: TransactionType, description: String) extends ClientActorCommand
  case class ClientGetStatementCommand(clientId: Int) extends ClientActorCommand

  // EventRegion
  trait ClientActorEvent
  case class ClientTransactionAddedEvent(clientId: Int, value: Int, transactionType: TransactionType, description: String) extends ClientActorEvent



  def props(clientId: Int): Props = Props(new ClientActor(clientId))

}

class ClientActor(val clientId: Int) extends Actor with ActorLogging {
  import ClientActor._
  import context._

  override def receive: Receive = handleCommands(ClientState(Client.initialWithId(clientId)))
    .orElse {
    case _ => log.warning(s"ClientActor received a message: ")
  }

  def handleCommands(state: ClientActorState): Receive = {
    state match {
      case s: ClientState => handleClientCommands(s)
      case ClientNoState => handleNoStateCommands
    }
  }

  def handleNoStateCommands: Receive = {
    case ClientAddTransactionCommand(clientId, value, transactionType, description) =>
      log.info(s"ClientActor with no state received a ClientAddTransactionCommand with clientId: $clientId, value: $value, transactionType: $transactionType, description: $description")
      val newState = ClientState(Client.initialWithId(clientId))
      val updatedClient = ClientAddTransactionHandler.handle().apply(newState.client, ClientAddTransactionCommand(clientId, value, transactionType, description))
      become(handleCommands(ClientState(updatedClient)))
  }

  def handleClientCommands(state: ClientState): Receive = {
    case ClientAddTransactionCommand(clientId, value, transactionType, description) =>
      log.info(s"ClientActor received a ClientAddTransactionCommand with clientId: $clientId, value: $value, transactionType: $transactionType, description: $description")
      val updatedClient = ClientAddTransactionHandler.handle().apply(state.client, ClientAddTransactionCommand(clientId, value, transactionType, description))
      become(handleCommands(ClientState(updatedClient)))

    case ClientGetStatementCommand(clientId) =>
      log.info(s"ClientActor received a ClientGetStatementCommand with clientId: $clientId")
      val client = state.client
      val statement = client.getStatement
      log.info(s"Client statement: $statement")
  }

}
