package com.akkamelo.api.adapter.endpoint

import akka.http.scaladsl.model.StatusCodes
import com.akkamelo.api.actor.client.ClientActor.{ClientActorPersistenceFailure, ClientActorResponse, ClientActorUnprocessableEntity, ClientAlreadyExists, ClientBalanceAndLimitResponse, ClientDoesntExist, ClientRegistered, ClientStatementResponse}
import com.akkamelo.api.actor.client.domain.state.TransactionType
import com.akkamelo.api.endpoint.dto._

object ActorResponse2ResponseDTO {
  def toResponseDTO(response: ClientActorResponse): ResponseDTO = {
    response match {
      case ClientStatementResponse(statement) =>
        ResponseDTO(
          StatusCodes.OK,
          Some(StatementResponseDTOPayload(
            BalanceDTO(statement.balanceInformation.balance, statement.balanceInformation.timestamp.toString, statement.balanceInformation.limit),
            statement.lastTransactions.map(t => TransactionDTO(t.value, transactionTypeToString(t.transactionType), t.description, t.timestamp.toString))
          ))
        )
      case ClientBalanceAndLimitResponse(balance, limit) =>
        ResponseDTO(StatusCodes.OK, Some(TransactionResponseDTOPayload(limit, balance)))

      case ClientDoesntExist=>
        ResponseDTO(StatusCodes.NotFound, None)
      case ClientAlreadyExists=>
        ResponseDTO(StatusCodes.UnprocessableEntity, None)
      case ClientRegistered(clientId) =>
        ResponseDTO(StatusCodes.OK, None)

      case ClientActorUnprocessableEntity =>
        ResponseDTO(StatusCodes.UnprocessableEntity, None)
      case ClientActorPersistenceFailure(message) =>
        ResponseDTO(StatusCodes.InternalServerError, None)
    }
  }

  private def transactionTypeToString(transactionType: TransactionType): String = {
    transactionType match {
      case TransactionType.DEBIT => "d"
      case TransactionType.CREDIT => "c"
    }
  }
}