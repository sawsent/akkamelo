package com.akkamelo.api.actor.client

import com.akkamelo.api.actor.client.domain.state.TransactionType

object ClientActor {

  // CommandRegion
  trait ClientCommand
  case class ClientAddTransactionCommand(clientId: Int, value: Int, transactionType: TransactionType, description: String) extends ClientCommand


}

class ClientActor {

}
