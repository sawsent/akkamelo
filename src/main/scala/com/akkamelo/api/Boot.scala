package com.akkamelo.api

import akka.actor.ActorSystem
import akka.util.Timeout
import com.akkamelo.api.actor.client.resolver.ClientActorResolver
import com.akkamelo.api.actor.greet.GreeterActor
import com.akkamelo.api.actor.greet.GreeterActor.{Configure, SayHello}
import com.akkamelo.api.endpoint.Server
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("no-conf")
  implicit val ec = system.dispatcher

  val config = ConfigFactory.load()
  implicit val clientActorResolverTimeout = Timeout(1.second)

  val greeter = system.actorOf(GreeterActor.props, "greeter")
  greeter ! Configure(config.getString("boot.message"))
  greeter ! SayHello

  val resolver = system.actorOf(ClientActorResolver.props, "client-actor-resolver")
  1 to 5 foreach { id =>
    resolver ! ClientActorResolver.RegisterClientActor(id)
  }

  val server: Server = new Server("localhost", 8080, resolver)
  val closeable = server.start()

  scala.io.StdIn.readLine()
  closeable.close()

}
