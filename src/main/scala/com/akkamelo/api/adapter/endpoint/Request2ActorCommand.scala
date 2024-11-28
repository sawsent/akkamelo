package com.akkamelo.api.adapter.endpoint

import com.akkamelo.api.actor.client.ClientActor.{ClientActorCommand, ClientAddTransactionCommand, ClientGetStatementCommand}
import com.akkamelo.api.actor.client.domain.state.{Transaction, TransactionType}
import com.akkamelo.api.http.dto.{ClientGetStatementRequestDTO, RequestDTO, TransactionRequestDTO}

object Request2ActorCommand {
  def toActorCommand(id: Int, request: RequestDTO): ClientActorCommand = request match {
    case TransactionRequestDTO(value, transactionType, description) => ClientAddTransactionCommand(id, value, stringToTransactionType(transactionType), description)
    case ClientGetStatementRequestDTO => ClientGetStatementCommand(id)
  }

  private def stringToTransactionType(transactionType: String): TransactionType = {
    transactionType match {
      case "d" => TransactionType.DEBIT
      case "c" => TransactionType.CREDIT
      case _ => TransactionType.NO_TYPE
    }
  }
}