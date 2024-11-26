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
    case any =>                   log.warning(s"Received unknown message: $any")
  }

  private def createOrRecoverClientActor(entityId: Int): ActorRef = {
    context.child(getActorName(entityId)) match {
      case Some(childActorRef) =>
        log.info(s"Found actor reference in memory for entityId: $entityId")
        childActorRef
      case None =>
        log.info(s"Creating actor reference for entityId: $entityId")
        context.actorOf(childPropsFactory(getPersistenceId(entityId)), getActorName(entityId))
    }
  }
}
