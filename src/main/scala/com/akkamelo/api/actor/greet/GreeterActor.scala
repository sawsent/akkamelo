package com.akkamelo.api.actor.greet

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.Logging
import com.akkamelo.api.actor.greet.GreeterActor.SayHello

object GreeterActor {
  def props(greeting: String): Props = Props(new GreeterActor(greeting))

  case object SayHello

}

class GreeterActor(val greeting: String) extends Actor with ActorLogging {

  override def receive: Receive = {
    case SayHello => log.info(greeting)
  }
}
