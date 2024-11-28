package com.akkamelo.api.http.dto

trait RequestDTO

case object ClientGetStatementRequestDTO extends RequestDTO
case class TransactionRequestDTO(valor: Int, tipo: String, descricao: String) extends RequestDTO