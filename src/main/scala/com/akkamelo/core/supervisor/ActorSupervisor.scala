package com.akkamelo.core.supervisor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.akkamelo.core.actor.BaseActor.ActorCommand
import com.akkamelo.core.supervisor.ActorSupervisor.{ActorNameFactory, ChildPropsFactory, PersistenceIdFactory}

object ActorSupervisor {
  type ChildPropsFactory = String => Props
  type ActorNameFactory = Int => String
  type PersistenceIdFactory = Int => String

  def props[C <: ActorCommand](childPropsFactory: ChildPropsFactory,
                               getPersistenceId: PersistenceIdFactory,
                               getActorName: ActorNameFactory,
                               ): Props =
    Props(new ActorSupervisor[C](getPersistenceId, childPropsFactory, getActorName))
}

class ActorSupervisor[C <: ActorCommand](val getPersistenceId: PersistenceIdFactory,
                                         val childPropsFactory: ChildPropsFactory,
                                         val getActorName: ActorNameFactory
                                         ) extends Actor with ActorLogging {

  override def receive: Receive = {
    case command: C @unchecked => createOrRecoverClientActor(command.entityId).forward(command)
    case any =>                   log.error(s"Received unknown message: $any")
  }

  private def createOrRecoverClientActor(entityId: Int): ActorRef = {
    context.child(getPersistenceId(entityId)) match {
      case Some(childActorRef) => childActorRef
      case None => context.actorOf(childPropsFactory(getPersistenceId(entityId)), getActorName(entityId))
    }
  }
}
