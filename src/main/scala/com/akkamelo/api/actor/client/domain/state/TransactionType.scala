package com.akkamelo.api.actor.client.domain.state

object TransactionType {
  case object CreditTransaction extends TransactionType
  case object DebitTransaction extends TransactionType
  case object NoTransactionType extends TransactionType

  val NO_TYPE: TransactionType = NoTransactionType
  val CREDIT: TransactionType = CreditTransaction
  val DEBIT: TransactionType = DebitTransaction

  def fromString(transactionType: String): TransactionType = transactionType match {
    case "c" => CREDIT
    case "d" => DEBIT
    case _ => NO_TYPE
  }
}

class TransactionType
