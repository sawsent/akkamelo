package com.akkamelo.api.actor.client.domain.state

object TransactionType {
  private case object CreditTransaction extends TransactionType {
    override def toStringRepresentation: String = "CREDIT"
  }
  private case object DebitTransaction extends TransactionType {
    override def toStringRepresentation: String = "DEBIT"
  }
  private case object NoTransactionType extends TransactionType {
    override def toStringRepresentation: String = "NO_TYPE"
  }

  val NO_TYPE: TransactionType = NoTransactionType
  val CREDIT: TransactionType = CreditTransaction
  val DEBIT: TransactionType = DebitTransaction

  val fromStringRepresentation: String => TransactionType = {
    case "CREDIT" => CreditTransaction
    case "DEBIT" => DebitTransaction
    case "NO_TYPE" => NoTransactionType
  }
}

trait TransactionType {
  def toStringRepresentation: String
}