package com.akkamelo.api.endpoint

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.akkamelo.api.actor.client.ClientActor.{ClientActorProcessingFailure, ClientActorResponse, ClientAddTransactionCommand, ClientBalanceAndLimitResponse, ClientGetStatementCommand, ClientStatementResponse}
import com.akkamelo.api.actor.client.domain.state.TransactionType
import com.akkamelo.api.actor.client.supervisor.ClientActorSupervisor.{ApplyCommand, NonExistingClientActor, ResolveClientActor}
import com.akkamelo.api.adapter.endpoint.{ActorResponse2ResponseDTO, Request2ActorCommand}
import com.akkamelo.api.endpoint.dto.{ClientGetStatementRequestDTO, RequestDTO, TransactionRequestDTO}
import com.akkamelo.api.endpoint.marshalling.CustomMarshalling
import com.akkamelo.core.logging.BaseLogging
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success


object Server {
  def apply(host: String, port: Int, clientActorResolver: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext, actorResolveTimeout: Timeout): UnstartedServer = new UnstartedServer(host, port, clientActorResolver)
}

trait Server {
  def start(): Server
  def close(): Unit
}

class UnstartedServer(val host: String, val port: Int, val clientActorResolver: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext, actorResolveTimeout: Timeout) extends Server {
  def start(): Server = new StartedServer(host, port, clientActorResolver)
  def close(): Unit = throw new IllegalStateException("Server is not started")
}

class StartedServer(host: String, port: Int, clientActorSupervisor: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext, actorResolveTimeout: Timeout) extends Server with BaseLogging with CustomMarshalling {
  def start(): Server = throw new IllegalStateException("Server is already started")


  val route: Route = {
    concat(
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

  private def handleRequest(clientId: Int, request: RequestDTO): Route = {
    val command = Request2ActorCommand(request)
    val responseFuture: Future[ClientActorResponse] = (clientActorSupervisor ? ApplyCommand(clientId, command)).mapTo[ClientActorResponse]

    onComplete(responseFuture) {
      case Success(response: ClientActorResponse) =>
        val responseDTO = ActorResponse2ResponseDTO(response)
        complete(responseDTO.code, responseDTO.payload)
      case _ =>
        complete(StatusCode.int2StatusCode(500), "Internal server error")
    }
  }

  private val bindingFuture = Http().newServerAt(host, port).bind(route)

  def close(): Unit = {
    bindingFuture.flatMap(_.unbind())
  }



}
