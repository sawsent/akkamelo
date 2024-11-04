package com.akkamelo.api.endpoint.dto

import akka.http.scaladsl.model.StatusCode

case class ResponseDTO(code: StatusCode, payload: Option[ResponseDTOPayload])

trait ResponseDTOPayload

case class StatementResponseDTOPayload(saldo: BalanceDTO, ultimas_transacoes: List[TransactionDTO]) extends ResponseDTOPayload
case class TransactionResponseDTOPayload(limite: Int, saldo: Int) extends ResponseDTOPayload

case class BalanceDTO(total: Int, data_extrato: String, limite: Int)
case class TransactionDTO(valor: Int, tipo: String, descricao: String, realizada_em: String)
