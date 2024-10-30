package com.akkamelo.api.endpoint

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.akkamelo.api.endpoint.dto.TransactionRequestDTO
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._

import scala.concurrent.ExecutionContext
import spray.json.DefaultJsonProtocol._
import JsonFormats._
import akka.http.scaladsl.model.HttpEntity
import akka.util.ByteString
import spray.json.RootJsonFormat

import scala.util.Success

class Server(host: String, port: Int)(implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) {

  val route: Route = {
    concat(
      pathPrefix("clientes" / Segment / "transacoes") { clientId =>
        post {
          entity(as[TransactionRequestDTO]) { request =>
            complete(s"POST /clientes/$clientId/transacoes")
          }
        }
      },
      pathPrefix("clientes" / Segment / "extrato") { clientId =>
        get {
          complete(s"GET /clientes/$clientId/extrato")
        }
      },

    )

  }

}
