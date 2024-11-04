package com.akkamelo.api.endpoint.dto

trait RequestDTO

case object ClientGetStatementRequestDTO extends RequestDTO
case class TransactionRequestDTO(valor: Int, tipo: String, descricao: String) extends RequestDTO

