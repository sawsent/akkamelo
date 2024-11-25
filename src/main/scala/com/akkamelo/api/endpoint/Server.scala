package com.akkamelo.api.endpoint

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.akkamelo.api.actor.client.ClientActor.{ClientActorCommand, ClientActorResponse}
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor.ApplyCommand
import com.akkamelo.api.adapter.endpoint.{ActorResponse2ResponseDTO, Request2ActorCommand}
import com.akkamelo.api.endpoint.dto.{ClientGetStatementRequestDTO, RequestDTO, TransactionRequestDTO}
import com.akkamelo.api.endpoint.marshalling.CustomMarshalling._
import com.akkamelo.core.logging.BaseLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object Server {
  def newStartedAt(host: String, port: Int, clientActorResolver: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext, actorResolveTimeout: Timeout): Server = new Server(host, port, clientActorResolver)
}

class Server(host: String, port: Int, clientActorSupervisor: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext, actorResolveTimeout: Timeout) extends BaseLogging {
  val route: Route = {
    concat(
      path("health") {
        get {
          logger.debug("Received request: 'GET /health'")
          complete(StatusCodes.OK, "Server is online")
        }
      },
      pathPrefix("health" / Segment) { id =>
        get {
          logger.debug(s"Received request: 'GET /health' from '$id'")
          complete(StatusCodes.OK, s"Server is online, $id")
        }
      },
      pathPrefix("clientes" / Segment / "transacoes") { clientId =>
        post {
          entity(as[TransactionRequestDTO]) { request =>
            logger.info(s"Received request: 'POST /clientes/$clientId/transacoes' with payload '$request'")
            handleRequest(clientId.toInt, request)
          }
        }
      },
      pathPrefix("clientes" / Segment / "extrato") { clientId =>
        get {
          logger.info(s"Received request: 'GET /clientes/$clientId/extrato'")
          handleRequest(clientId.toInt, ClientGetStatementRequestDTO)
        }
      }
    )
  }

  def handleRequest(clientId: Int, request: RequestDTO): Route = {
    val command = Request2ActorCommand(request)
    val responseFuture: Future[ClientActorResponse] = (clientActorSupervisor ? ApplyCommand(clientId, command)).mapTo[ClientActorResponse]

    onComplete(responseFuture) {
      case Success(response: ClientActorResponse) =>
        val responseDTO = ActorResponse2ResponseDTO.toResponseDTO(response)
        complete(responseDTO.code, responseDTO.payload)
      case any =>
        logger.error(s"Failed to process request: $any")
        complete(StatusCode.int2StatusCode(500), "Internal server error")
    }
  }

  Http().newServerAt(host, port).bind(route).onComplete {
    case Success(_) => logger.info(s"Server online at http://$host:$port/")
    case _ => logger.error("Failed to start server")
  }
}
