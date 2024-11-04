package com.akkamelo.api.adapter.endpoint

import com.akkamelo.api.actor.client.ClientActor
import com.akkamelo.api.actor.client.ClientActor.{ClientActorResponse, ClientStatementResponse}
import com.akkamelo.api.actor.client.domain.state.TransactionType
import com.akkamelo.api.endpoint.dto.{BalanceDTO, ResponseDTO, StatementResponseDTO, TransactionDTO, TransactionResponseDTO}

object ActorResponse2ResponseDTO {
  def apply(response: ClientActorResponse): ResponseDTO = {
    response match {
      case ClientStatementResponse(statement) =>
        StatementResponseDTO(
          BalanceDTO(statement.balanceInformation.balance, statement.balanceInformation.timestamp.toString, statement.balanceInformation.limit),
          statement.lastTransactions.map(t => TransactionDTO(t.value, transactionTypeToString(t.transactionType), t.description, t.timestamp.toString))
        )
      case ClientActor.ClientBalanceAndLimitResponse(balance, limit) => TransactionResponseDTO(limit, balance)
    }
  }

  private def transactionTypeToString(transactionType: TransactionType): String = {
    transactionType match {
      case TransactionType.DEBIT => "d"
      case TransactionType.CREDIT => "c"
    }
  }


}
