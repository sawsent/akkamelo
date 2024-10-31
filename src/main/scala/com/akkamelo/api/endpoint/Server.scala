package com.akkamelo.api.endpoint

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.akkamelo.api.endpoint.dto.TransactionRequestDTO
import com.akkamelo.core.logging.BaseLogging
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

class Server(host: String, port: Int, clientActorResolver: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext) extends BaseLogging {

  implicit val transactionRequestFormat: RootJsonFormat[TransactionRequestDTO] = jsonFormat3(TransactionRequestDTO)

  val route: Route = {
    concat(
      pathPrefix("clientes" / Segment / "transacoes") { clientId =>
        post {
          entity(as[TransactionRequestDTO]) { request =>
            logger.info(s"Received request: 'POST /clientes/$clientId/transacoes' with payload '$request'")
            complete(s"POST /clientes/$clientId/transacoes")
          }
        }
      },
      pathPrefix("clientes" / Segment / "extrato") { clientId =>
        get {
          logger.info(s"Received request: 'GET /clientes/$clientId/extrato'")
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
