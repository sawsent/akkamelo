package com.akkamelo.api.actor.persistencetest

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import com.akkamelo.api.actor.persistencetest.PersistentTestActor.PersistCmd

object PersistentTestActor {
  case class PersistCmd(payload: String)

  def props(persistenceId: String): Props = Props(new PersistentTestActor(persistenceId))
}

class PersistentTestActor(persistenceIdval: String) extends PersistentActor with ActorLogging {
  override def persistenceId: String = persistenceIdval

  override def receiveRecover: Receive = {
    case any: String => log.info("Recovered " + any)
    case _ => log.error("Unknown message received during recovery")
  }

  override def receiveCommand: Receive = {
    case PersistCmd(payload) => persist(payload) { payload => log.info("Persisted " + payload) }
    case _ => log.error("Unknown message received")
  }

}
