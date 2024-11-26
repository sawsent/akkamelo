package com.akkamelo.api.actor.client

import akka.actor.ActorSystem
import com.akkamelo.api.actor.common.BaseActorSpec

class ClientActorSpec extends BaseActorSpec(ActorSystem("ClientActorSpec")) {

  "A client Actor" should "reply with the balance and limit after adding a transaction" in {
    throw new NotImplementedError("Not implemented")
  }

  it should "reply with the statement when requested" in {
    throw new NotImplementedError("Not implemented")
  }

  it should "reply with an ActorProcessingFailure when the Transaction doesn't go through and ClientState should stay the same" in {
    throw new NotImplementedError("Not implemented")
  }

  it should "reply with an ActorNotFound when the ClientState doesn't exist" in {
    throw new NotImplementedError("Not implemented")
  }

  it should "reply with an ActorAlreadyExists when the ClientState already exists" in {
    throw new NotImplementedError("Not implemented")
  }

}

object ClientActorSpec {

}
