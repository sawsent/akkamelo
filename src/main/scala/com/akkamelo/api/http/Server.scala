package com.akkamelo.api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.akkamelo.api.http.dto.{ClientGetStatementRequestDTO, ClientRegisterRequestDTO, TransactionRequestDTO}
import com.akkamelo.api.http.marshalling.CustomMarshalling._

import scala.concurrent.ExecutionContext
import scala.util.Success


object Server {
  def newStartedAt(host: String, port: Int, service: ClientService)
                  (implicit system: ActorSystem, ec: ExecutionContext): Server = new Server(host, port, service)
}

class Server(host: String, port: Int, service: ClientService)(implicit system: ActorSystem, ec: ExecutionContext) {

  val route: Route = {
    concat(
      path("health") {
        get {
          complete(StatusCodes.OK, "Server is online")
        }
      },
      pathPrefix("clientes" / Segment / "transacoes") { clientId =>
        post {
          entity(as[TransactionRequestDTO]) { request =>
            val response = service.handleRequest(clientId.toInt, request)
            complete(response.code, response.payload)
          }
        }
      },
      pathPrefix("clientes" / Segment / "extrato") { clientId =>
        get {
          val response = service.handleRequest(clientId.toInt, ClientGetStatementRequestDTO)
          complete(response.code, response.payload)
        }
      },
      pathPrefix("clientes" / Segment / "registar") { clientId =>
        post {
          entity(as[ClientRegisterRequestDTO]) { request =>
            val response = service.handleRequest(clientId.toInt, request)
            complete(response.code, response.payload)
          }
        }
      }
    )
  }

  Http().newServerAt(host, port).bind(route).onComplete {
    case Success(_) => println(s"Server online at http://$host:$port/")
    case _ => println("Failed to start server")
  }
}
