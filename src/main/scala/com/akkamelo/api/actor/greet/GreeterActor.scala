package com.akkamelo.api.actor.greet

import akka.actor.{Actor, ActorLogging, Props}
import com.akkamelo.api.actor.greet.GreeterActor.{ConfigurationSuccess, Configure, SayHello}

object GreeterActor {
  def props: Props = Props(new GreeterActor)

  case object SayHello
  case class Configure(greeting: String)
  case class ConfigurationSuccess(greeting: String)

}

class GreeterActor extends Actor with ActorLogging {

  def configurable: Receive = {
    case Configure(greeting) =>
      sender() ! ConfigurationSuccess(greeting)
      context.become(configured(greeting))
  }

  override def receive: Receive = configurable.orElse {
    case any: Any =>
      log.warning(s"Received ${any.toString} before being configured.")
  }

  def configured(greeting: String): Receive = configurable.orElse {
    case SayHello => log.info(greeting)
  }
}
