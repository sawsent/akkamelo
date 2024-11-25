package com.akkamelo.api.actor.greeter

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{EventFilter, TestKit, TestProbe}
import com.akkamelo.api.actor.common.BaseActorSpec
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{ConfigurationSuccess, Configure, SayHello}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class GreeterActorSpec extends BaseActorSpec(ActorSystem("GreeterActorSpec")) {
  import GreeterActorSpec._

  "an Unconfigured GreeterActor" should "not reply with anything when successfully configured" in {
    val greeterActorRef = resetGreeter(system, "g1")
    val testProbe = TestProbe()
    val greeting = "greeting"

    greeterActorRef.tell(Configure(greeting), testProbe.ref)

    testProbe.expectNoMessage()
  }

  it should "log that it tried to be used before configuration when sent anything else" in {
    val greeterActorRef = resetGreeter(system, "g2")
    val testProbe = TestProbe()

    val any = "greeting"
    EventFilter.warning(pattern = s"Received ${any.toString} before being configured.") intercept {
      greeterActorRef.tell(any, testProbe.ref)
      testProbe.expectNoMessage()
    }

    val any2 = SayHello
    EventFilter.warning(pattern = s"Received ${any2.toString} before being configured.") intercept {
      greeterActorRef.tell(any2, testProbe.ref)
      testProbe.expectNoMessage()
    }

  }

  "A greeter actor configured with the greeting: 'Hello Testkit'" should "log a message with the configured greeting" in {
    val greeting = "Hello TestKit"
    val testProbe = TestProbe()
    val configuredGreeterActor = getConfiguredGreeterActor(system, "cg1", greeting, testProbe)

    configuredGreeterActor.tell(Configure(greeting), testProbe.ref)
    EventFilter.info(pattern = s"\\Q$greeting\\E") intercept {
      configuredGreeterActor.tell(SayHello, testProbe.ref)
    }
  }


  it should "not reply with any messages" in {

    val greeting = "Hello TestKit"
    val testProbe = TestProbe()
    val configuredGreeterActor = getConfiguredGreeterActor(system, "cg2", greeting, testProbe)
    configuredGreeterActor.tell(Configure(greeting), testProbe.ref)

    configuredGreeterActor.tell(SayHello, testProbe.ref)
    testProbe.expectNoMessage()
  }

}

object GreeterActorSpec {
  val testActorProps: Props = Props(new GreeterActor)

  val resetGreeter: (ActorSystem, String) => ActorRef = (system: ActorSystem, name: String) =>
    system.actorOf(testActorProps, name)

  def getConfiguredGreeterActor(system: ActorSystem, name: String, greeting: String, testProbe: TestProbe): ActorRef = {
    val actorRef = resetGreeter(system, name)
    actorRef.tell(Configure(greeting), testProbe.ref)
    testProbe.expectNoMessage()
    actorRef
  }
}
