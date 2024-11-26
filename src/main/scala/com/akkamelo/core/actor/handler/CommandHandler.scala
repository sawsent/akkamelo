package com.akkamelo.core.actor.handler

import com.akkamelo.core.actor.BaseActor.ActorCommand
import com.akkamelo.core.actor.state.ActorState

trait CommandHandler[S <: ActorState, C <: ActorCommand] {
  type Handler = PartialFunction[(S, C), S]
  def handle: Handler
}
