package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.AssignClientCommand
import com.akkamelo.api.actor.client.domain.state.{Client, ClientActorState, ClientNoState, ClientState}

class ClientAssignClientHandler {
  import ClientAssignClientHandler.Handler

  def handle(): Handler = {
    case (ClientNoState, AssignClientCommand(clientId, initialLimit, initialBalance)) =>
      ClientState(Client.initialWithId(clientId).copy(limit = initialLimit, balanceSnapshot = initialBalance))
    case (_: ClientState, _) =>
      throw new IllegalStateException("Client already assigned.")
  }

}
object ClientAssignClientHandler {
  type Handler = PartialFunction[(ClientActorState, AssignClientCommand), ClientState]
  def apply(): ClientAssignClientHandler = new ClientAssignClientHandler()
}
