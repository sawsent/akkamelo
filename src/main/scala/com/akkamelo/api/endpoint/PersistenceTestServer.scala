package com.akkamelo.api.endpoint

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.akkamelo.api.actor.persistencetest.PersistentTestActor.PersistCmd
import com.akkamelo.core.logging.BaseLogging
import spray.json.DefaultJsonProtocol.{StringJsonFormat, jsonFormat1}
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object PersistenceTestServer {
  def apply(host: String, port: Int, persistenceTestActor: ActorRef)(implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext): PersistenceTestServer =
    new PersistenceTestServer(host, port, persistenceTestActor)
}

class PersistenceTestServer(host: String, port: Int, val persistenceTestActor: ActorRef)(implicit val system: ActorSystem, val materializer: Materializer, val ec: ExecutionContext) extends BaseLogging {
  case class PersistRequest(payload: String)
  implicit val persistRequestFormat: RootJsonFormat[PersistRequest] = jsonFormat1(PersistRequest)

  val route: Route = path("persist-test") {
    post {
      entity(as[PersistRequest]) { request =>
        logger.info(s"Received request: 'POST /persist-test' with payload '${request.payload}'")

        persistenceTestActor ! PersistCmd(request.payload)

        complete((StatusCodes.OK, s"Received payload: ${request.payload}"))
      }
    }
  }

  Http().newServerAt(host, port).bind(route).onComplete {
    case Success(_) => logger.info(s"Persistence Test Server started at $host:$port")
    case Failure(exception) => logger.error(s"Failed to start Persistence Test server at $host:$port", exception)
  }

}
