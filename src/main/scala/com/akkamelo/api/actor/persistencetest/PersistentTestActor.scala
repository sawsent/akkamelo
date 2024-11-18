package com.akkamelo.api.actor.persistencetest

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

object PersistentTestActor {
  case class PersistCmd(payload: String)

  def props(persistenceId: String): Props = Props(new PersistentTestActor(persistenceId))
}

class PersistentTestActor(persistenceIdval: String) extends PersistentActor with ActorLogging {
  override def persistenceId: String = persistenceIdval

  override def receiveRecover: Receive = {
    case _ => log.info("Recovered")
  }

  override def receiveCommand: Receive = {
    case _ => persist("event") { _ => log.info("Persisted") }
  }

}
