package com.akkamelo.api.actor.client.domain.state

import com.akkamelo.core.actor.state.ActorState

trait ClientActorState extends ActorState {
  def toFullState: ClientState = this match {
    case state: ClientState => state
    case _ => throw new IllegalStateException("ClientActorState is not a ClientState")
  }
}

case class ClientState(client: Client) extends ClientActorState
case object ClientNoState extends ClientActorState