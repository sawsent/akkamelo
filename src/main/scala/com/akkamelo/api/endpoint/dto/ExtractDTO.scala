package com.akkamelo.api.endpoint.dto



case class ExtractResponseDTO(saldo: BalanceDTO, ultimas_transacoes: List[TransactionDTO])

case class BalanceDTO(total: Int, data_extrato: String, limite: Int)