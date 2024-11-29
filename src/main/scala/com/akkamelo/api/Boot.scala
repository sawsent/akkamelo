package com.akkamelo.api

import com.akkamelo.api.http.{ClientService, Server}

object Boot extends App {
  println("Starting Akkamelo")

  val host = Config.server("host").asInstanceOf[String]
  val port = Config.server("port").asInstanceOf[Int]
  val server = startServer("localhost", 8080, ClientService())

  def startServer(host: String, port: Int, service: ClientService): Server = Server.newStartedAt(host, port, service)
}

object Config {
  val server = Map("host" -> "localhost", "port" -> 8080)
}