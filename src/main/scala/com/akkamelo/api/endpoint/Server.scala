package com.akkamelo.api.endpoint

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.akkamelo.api.actor.client.ClientActor.{ClientAddTransactionCommand, ClientGetStatementCommand}
import com.akkamelo.api.actor.client.domain.state.TransactionType
import com.akkamelo.api.actor.client.resolver.ClientActorResolver.ResolveClientActor
import com.akkamelo.api.endpoint.dto.TransactionRequestDTO
import com.akkamelo.core.logging.BaseLogging
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class Server(host: String, port: Int, clientActorResolver: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext, actorResolveTimeout: Timeout) extends BaseLogging {

  implicit val transactionRequestFormat: RootJsonFormat[TransactionRequestDTO] = jsonFormat3(TransactionRequestDTO)

  val route: Route = {
    concat(
      pathPrefix("clientes" / Segment / "transacoes") { clientId =>
        post {
          entity(as[TransactionRequestDTO]) { request =>
            logger.info(s"Received request: 'POST /clientes/$clientId/transacoes' with payload '$request'")

            val clientActorRefFuture = (clientActorResolver ? ResolveClientActor(clientId.toInt)).mapTo[Option[ActorRef]]
            clientActorRefFuture.onComplete {
              case Success(value) => value match {
                case Some(clientActorRef: ActorRef) =>
                  clientActorRef ! ClientAddTransactionCommand(clientId.toInt, request.valor, TransactionType.fromString(request.tipo), request.descricao)
                  complete(s"POST /clientes/$clientId/transacoes success")
                case None =>
                  complete(s"client $clientId not found")
              }
              case _ => "error"
            }
            complete(s"POST /clientes/$clientId/transacoes")
          }
        }
      },
      pathPrefix("clientes" / Segment / "extrato") { clientId =>
        get {
          logger.info(s"Received request: 'GET /clientes/$clientId/extrato'")
          val clientActorRefFuture = (clientActorResolver ? ResolveClientActor(clientId.toInt)).mapTo[Option[ActorRef]]
          clientActorRefFuture.onComplete {
            case Success(value) => value match {
              case Some(clientActorRef: ActorRef) =>
                clientActorRef ! ClientGetStatementCommand(clientId.toInt)
              case None =>
                logger.info(s"client $clientId not found")
            }
          }
          complete(s"GET /clientes/$clientId/extrato")
        }
      },
    )
  }



  def start(): CloseableServer = {
    val out: CloseableServer = CloseableServer(Http().newServerAt("localhost", 8080).bind(route))
    logger.info(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    out
  }

  object CloseableServer {
    def apply(bindingFuture: Future[Http.ServerBinding]): CloseableServer = new CloseableServer(bindingFuture)
  }

  class CloseableServer(val bindingFuture: Future[Http.ServerBinding]) {
    def close(): Unit = {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    }
  }
}
