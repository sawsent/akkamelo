package com.akkamelo.api.actor.common

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class BaseActorSpec(_system: ActorSystem) extends TestKit(_system)
  with AnyFlatSpecLike with BeforeAndAfterAll with Matchers with ImplicitSender with GivenWhenThen
  with TableDrivenPropertyChecks {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

}
