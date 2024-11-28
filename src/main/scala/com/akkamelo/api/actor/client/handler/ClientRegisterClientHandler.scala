package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.RegisterClient
import com.akkamelo.api.actor.client.domain.state.{Client, ClientActorState, ClientNoState, ClientState}
import com.akkamelo.core.actor.handler.CommandHandler

class ClientRegisterClientHandler extends CommandHandler[ClientActorState, RegisterClient] {
  override def handle: Handler = {
    case (ClientNoState, RegisterClient(clientId, initialLimit, initialBalance)) =>
      ClientState(Client.initialWithId(clientId).copy(limit = initialLimit, balanceSnapshot = initialBalance))
    case (_: ClientState, _) =>
      throw new IllegalStateException("Client already assigned.")
  }
}

object ClientRegisterClientHandler {
  def apply(): ClientRegisterClientHandler = new ClientRegisterClientHandler()
}