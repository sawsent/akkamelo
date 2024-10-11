package com.akkamelo.api.actor.client.domain.state

import com.akkamelo.api.actor.client.exception.{InvalidTransactionException, TransactionConversionException}

import java.time.LocalDateTime

object Transaction {

  def apply(
             value: Int,
             description: String,
             transactionType: TransactionType,
             timestamp: LocalDateTime = LocalDateTime.now()
           ): Transaction =
  {
    if (value < 0) throw InvalidTransactionException("Transaction with negative value cannot exist")
    if (description.isEmpty || description.length > 10) throw InvalidTransactionException("Description length must have length between 1 and 10 characters")
    if (transactionType == TransactionType.NO_TYPE) throw InvalidTransactionException("Transaction type must be specified.")

    new Transaction(value, description, transactionType, timestamp)
  }

  def newCreditTransaction(value: Int, description: String): Credit = Transaction(value, description, TransactionType.CREDIT).toCredit
  def newDebitTransaction(value: Int, description: String): Debit = Transaction(value, description, TransactionType.DEBIT).toDebit
}

class Transaction(val value: Int, val description: String, val transactionType: TransactionType, val timestamp: LocalDateTime) {
  private def toCredit: Credit = transactionType match {
    case TransactionType.CREDIT => new Credit(value, description, timestamp)
    case _ => throw TransactionConversionException("Can't convert debit transaction to credit transaction.")
  }

  private def toDebit: Debit = transactionType match {
    case TransactionType.DEBIT => new Debit(value, description, timestamp)
    case _ => throw TransactionConversionException("Can't convert credit transaction to debit transaction.")
  }

  override def equals(obj: Any): Boolean = obj match {
    case t: Transaction => t.value == value && t.description == description && t.transactionType == transactionType
    case _ => false
  }
}

object Credit {
  def apply(value: Int, description: String): Credit = Transaction.newCreditTransaction(value, description)
}
case class Credit(override val value: Int, override val description: String, override val timestamp: LocalDateTime)
  extends Transaction(value, description, TransactionType.CREDIT, timestamp)

object Debit {
  def apply(value: Int, description: String): Debit = Transaction.newDebitTransaction(value, description)
}
case class Debit(override val value: Int, override val description: String, override val timestamp: LocalDateTime)
  extends Transaction(value, description, TransactionType.DEBIT, timestamp)


