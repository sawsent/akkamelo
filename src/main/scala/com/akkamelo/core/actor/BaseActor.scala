package com.akkamelo.core.actor

object BaseActor {
  // CommandRegion
  trait ActorCommand extends Serializable {
    def entityId: Int
  }

  // EventRegion
  trait ActorEvent extends Serializable {
    def entityId: Int
  }
}