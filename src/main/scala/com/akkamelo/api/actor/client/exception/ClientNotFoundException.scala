package com.akkamelo.api.actor.client.exception

case class ClientNotFoundException(message: String) extends RuntimeException(message)

