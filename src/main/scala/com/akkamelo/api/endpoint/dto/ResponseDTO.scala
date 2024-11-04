package com.akkamelo.api.endpoint.dto

trait ResponseDTO

case object ClientNotFoundResponseDTO extends ResponseDTO {
  override def toString: String = "Client not found"
}

case class StatementResponseDTO(saldo: BalanceDTO, ultimas_transacoes: List[TransactionDTO]) extends ResponseDTO
case class TransactionResponseDTO(limite: Int, saldo: Int) extends ResponseDTO

case class BalanceDTO(total: Int, data_extrato: String, limite: Int)
case class TransactionDTO(valor: Int, tipo: String, descricao: String, realizada_em: String)
