package com.akkamelo.api.actor.client.exception

case class InvalidTransactionException(message: String) extends RuntimeException(message)
