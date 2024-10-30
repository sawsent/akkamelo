package com.akkamelo.api.endpoint.dto

case class TransactionDTO(valor: Int, tipo: String, descricao: String, realizada_em: String)

case class TransactionRequestDTO(valor: Int, tipo: String, descricao: String)

case class TransactionResponseDTO(limite: Int, saldo: Int)
