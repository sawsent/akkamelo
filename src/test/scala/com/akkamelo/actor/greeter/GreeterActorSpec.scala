package com.akkamelo.actor.greeter

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{EventFilter, TestKit, TestProbe}
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{ConfigurationSuccess, Configure, SayHello}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class GreeterActorSpec extends TestKit(ActorSystem("GreeterActorSpec")) with AnyWordSpecLike with BeforeAndAfterAll {

  import GreeterActorSpec._

  "A greeter Actor before configuration" should {

    "When sent a Configuration object" should {

      "reply with ConfigurationSuccess" in {
        val testProbe = TestProbe()
        val testGreeter = resetGreeter(system, "greeter1")
        val greeting = "greeting"

        testGreeter.tell(Configure(greeting), testProbe.ref)

        testProbe.expectNoMessage(1.seconds)
      }

    }

    "When sent anything else" should {

      "log that it received a message before configuration" in {
        val testProbe = TestProbe()
        val testGreeter = resetGreeter(system, "greeter9")

        List[Any](SayHello, "test", 2, SayHello).foreach( any => {
          EventFilter.warning(pattern = s"Received $any before being configured.") intercept {
            testGreeter.tell(SayHello, testProbe.ref)
          }
        })

      }
    }
  }

  "A greeter actor configured with the greeting: 'Hello Testkit'" should {

    "when receiving a SayHello object" should {

      "Log a message with the configured greeting" in {
        val testProbe = TestProbe()
        val greeting = "Hello TestKit"
        val greeterActor = resetGreeter(system, "greeter2")

        greeterActor.tell(Configure(greeting), testProbe.ref)

        EventFilter.info(pattern = s"\\Q$greeting\\E") intercept {
          greeterActor.tell(SayHello, testProbe.ref)
        }

      }


      "not reply with any messages" in {
        val testProbe = TestProbe()
        val greeting = "Hello TestKit"
        val greeterActor = resetGreeter(system, "greeter3")

        greeterActor.tell(Configure(greeting), testProbe.ref)
        testProbe.expectMsg(ConfigurationSuccess(greeting))

        greeterActor.tell(SayHello, testProbe.ref)
        testProbe.expectNoMessage()
      }
    }
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}

object GreeterActorSpec {
  val testActorProps: Props = Props(new GreeterActor)

  val resetGreeter: (ActorSystem, String) => ActorRef = (system: ActorSystem, name: String) =>
    system.actorOf(testActorProps, name)
}
