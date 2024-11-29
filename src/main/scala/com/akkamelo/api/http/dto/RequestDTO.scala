package com.akkamelo.api.http.dto

trait RequestDTO

case object ClientGetStatementRequestDTO extends RequestDTO
case class TransactionRequestDTO(valor: Int, tipo: String, descricao: String) extends RequestDTO
case class ClientRegisterRequestDTO(saldo_inicial: Int, limite: Int) extends RequestDTO