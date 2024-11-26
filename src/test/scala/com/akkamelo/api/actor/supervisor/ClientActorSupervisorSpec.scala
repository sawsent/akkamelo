package com.akkamelo.api.actor.supervisor

import akka.actor.ActorSystem
import com.akkamelo.api.actor.common.BaseActorSpec

import scala.concurrent.ExecutionContext

class ClientActorSupervisorSpec extends BaseActorSpec(ActorSystem("ClientActorSupervisorSpec")) {
  implicit val ec: ExecutionContext = system.dispatcher


}