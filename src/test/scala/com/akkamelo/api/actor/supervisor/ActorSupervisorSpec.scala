package com.akkamelo.api.actor.supervisor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{EventFilter, TestProbe}
import com.akkamelo.api.actor.common.BaseActorSpec
import com.akkamelo.api.actor.supervisor.ActorSupervisorSpec.{MockedActorCommand, getActorExists, mockActorNameFactory, resetSupervisor}
import com.akkamelo.core.actor.BaseActor.ActorCommand
import com.akkamelo.core.supervisor.ActorSupervisor

import scala.concurrent.ExecutionContext

class ActorSupervisorSpec extends BaseActorSpec(ActorSystem("ClientActorSupervisorSpec")) {
  implicit val ec: ExecutionContext = system.dispatcher

  "Actor Supervisor" should "forward messages to the child actor if they extend ActorCommand" in {
    val name = "supervisor-victim-0"
    val receiverProbe = TestProbe()
    val senderProbe = TestProbe()
    val supervisor = ActorSupervisorSpec.resetSupervisor(name, receiverProbe.ref)

    val entityId = 1
    val cmd = MockedActorCommand(entityId)

    supervisor.tell(cmd, senderProbe.ref)
    receiverProbe.expectMsg(cmd)
    assert(receiverProbe.lastSender == senderProbe.ref)
  }

  it should "create an actor if it doesn't exist yet" in {
    val name = "supervisor-victim-1"
    val entityId = 1
    val expectedActorName = mockActorNameFactory(entityId)

    assert(!getActorExists(s"/user/$name/$expectedActorName"))

    val receiverProbe = TestProbe()
    val senderProbe = TestProbe()
    val supervisor = ActorSupervisorSpec.resetSupervisor(name, receiverProbe.ref)

    val cmd = MockedActorCommand(entityId)

    supervisor.tell(cmd, senderProbe.ref)

    receiverProbe.expectMsg(cmd)

    assert(getActorExists(s"/user/$name/$expectedActorName"))
  }

  it should "use the same ref if it already exists" in {
    val name = "supervisor-victim-2"
    val entityId = 1
    val expectedActorName = mockActorNameFactory(entityId)

    assert(!getActorExists(s"/user/$name/$expectedActorName"))

    val receiverProbe = TestProbe()
    val senderProbe = TestProbe()
    val supervisor = ActorSupervisorSpec.resetSupervisor(name, receiverProbe.ref)

    val cmd = MockedActorCommand(entityId)

    EventFilter.info().intercept {
      supervisor.tell(cmd, senderProbe.ref)
    }
    receiverProbe.expectMsg(cmd)
    assert(getActorExists(s"/user/$name/$expectedActorName"))

    EventFilter.info().intercept {
      supervisor.tell(cmd, senderProbe.ref)
    }
    receiverProbe.expectMsg(cmd)
  }

  it should "warn that it received an unknown message and ignore it" in {
    val name = "supervisor-victim-3"
    val receiverProbe = TestProbe()
    val senderProbe = TestProbe()
    val supervisor = ActorSupervisorSpec.resetSupervisor(name, receiverProbe.ref)

    EventFilter.warning().intercept {
      supervisor.tell("unknown message", senderProbe.ref)
    }
    EventFilter.warning().intercept {
      supervisor.tell(List(), senderProbe.ref)
    }
    EventFilter.warning().intercept {
      supervisor.tell(Option("Hello"), senderProbe.ref)
    }
    EventFilter.warning().intercept {
      supervisor.tell(Some(MockedActorCommand(1)), senderProbe.ref)
    }
    EventFilter.warning().intercept {
      supervisor.tell("this should make a warning instead", senderProbe.ref)
    }

    senderProbe.expectNoMessage()
  }




}

object ActorSupervisorSpec {
  val mockPersistenceIdFactory: Int => String = (id: Int) => s"test-persistence-id-$id"
  val mockActorNameFactory: Int => String = (id: Int) => s"test-actor-name-$id"
  val mockChildPropsFactory: ActorRef => String => Props = (probeRef: ActorRef) => _ => TestChildActor.props(probeRef)

  def resetSupervisor(name: String, probeRef: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext): ActorRef =
    system.actorOf(ActorSupervisor.props(mockChildPropsFactory(probeRef), mockPersistenceIdFactory, mockActorNameFactory), name)

  def getActorExists(path: String)(implicit system: ActorSystem): Boolean = {
    val senderProbe = TestProbe()
    val childSelection = system.actorSelection(path)

    childSelection.tell(akka.actor.Identify(None), senderProbe.ref)
    val actorIdentity = senderProbe.expectMsgType[akka.actor.ActorIdentity]
    actorIdentity.ref.isDefined
  }

  case class MockedActorCommand(entityId: Int) extends ActorCommand

  object TestChildActor {
    def props(probeRef: ActorRef): Props = Props(new TestChildActor(probeRef))
  }
  class TestChildActor(val probeRef: ActorRef) extends Actor {
    override def receive: Receive = {
      case any => probeRef.forward(any)
    }
  }
}