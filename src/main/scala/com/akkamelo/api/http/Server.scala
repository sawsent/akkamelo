package com.akkamelo.api.http

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.akkamelo.api.actor.client.ClientActor.ClientActorResponse
import com.akkamelo.api.adapter.endpoint.{ActorResponse2ResponseDTO, Request2ActorCommand}
import com.akkamelo.api.http.dto.{ClientGetStatementRequestDTO, RequestDTO, TransactionRequestDTO}
import com.akkamelo.api.http.marshalling.CustomMarshalling._
import com.akkamelo.core.logging.BaseLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success


object Server {
  def newStartedAt(host: String, port: Int, clientActorSupervisorRef: ActorRef)
                  (implicit system: ActorSystem, ec: ExecutionContext, actorResolveTimeout: Timeout): Server = new Server(host, port, clientActorSupervisorRef)
}

class Server(host: String, port: Int, clientActorSupervisor: ActorRef)
            (implicit system: ActorSystem, ec: ExecutionContext, actorRequestTimeout: Timeout) extends BaseLogging {

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
    val command = Request2ActorCommand.toActorCommand(clientId, request)
    val responseFuture: Future[ClientActorResponse] = (clientActorSupervisor ? command).mapTo[ClientActorResponse]

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
