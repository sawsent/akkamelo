package com.akkamelo.api.actor.client.domain.state

class ClientActorState

case class ClientState(client: Client) extends ClientActorState
case object ClientNoState extends ClientActorState