package com.akkamelo.api.actor.client.handler

import com.akkamelo.api.actor.client.ClientActor.RegisterClient
import com.akkamelo.api.actor.client.domain.state.{Client, ClientNoState, ClientState}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should
import org.scalatest.prop.TableDrivenPropertyChecks

class ClientRegisterClientCommandHandlerSpec extends AnyFlatSpecLike with TableDrivenPropertyChecks with should.Matchers {
  "RegisterClientHandler" should "create a new Client with the provided values, if no state exists yet" in {

    val victim = ClientRegisterClientHandler().handle
    val examples = Table(("description", "clientState", "transactionCommand", "expectation"),
      (
        "Initial Client 1",
        ClientNoState,
        RegisterClient(entityId = 1, initialLimit = 100, initialBalance = 100),
        ClientState(Client.initialWithId(1).copy(limit = 100, balanceSnapshot = 100))
      ),
      (
        "Initial Client 2",
        ClientNoState,
        RegisterClient(entityId = 2, initialLimit = 100, initialBalance = 100),
        ClientState(Client.initialWithId(2).copy(limit = 100, balanceSnapshot = 100))
      )
    )
  }

  "ClientRegisterClientHandler" should "throw IllegalStateException if a client is already assigned" in {
    val victim = ClientRegisterClientHandler().handle
    assertThrows[IllegalStateException](victim(ClientState(Client.initialWithId(1)), RegisterClient(1, 100, 100)))
    assertThrows[IllegalStateException](victim(ClientState(Client.initialWithId(2)), RegisterClient(2, 100, 100)))
  }
}