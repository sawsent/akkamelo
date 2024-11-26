package com.akkamelo.core.actor.converter

import com.akkamelo.core.actor.BaseActor.{ActorCommand, ActorEvent}

trait ActorCommand2ActorEvent[C <: ActorCommand, E <: ActorEvent] {
  def toActorEvent(command: C): E
}
