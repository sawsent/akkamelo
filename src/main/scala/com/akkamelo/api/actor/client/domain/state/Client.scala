package com.akkamelo.api.actor.client.domain.state

import com.akkamelo.api.actor.client.exception.InvalidTransactionException
import java.time.LocalDateTime


object Client {
  def initialWithId(id: Int): Client = Client.initial.copy(id = id)
  val initial: Client = Client(0, List.empty[Transaction], 0)
}

case class Client(id: Int, transactions: List[Transaction], limit: Int, balanceSnapshot: Int = 0) {
  def add(transaction: Transaction): Client = {
    transaction match {
      case c: Credit =>
      case d: Debit => if (balance - d.value < -1 * limit) throw InvalidTransactionException("Can't debit to lower than limit")
    }
    if (transactions.length >= 100) {
      val newBalanceSnapshot = balanceSnapshot + balance(transactions.drop(10))
      copy(transactions = transaction +: transactions.take(10), balanceSnapshot = newBalanceSnapshot)
    } else {
      copy(transactions = transaction +: transactions)
    }
  }

  def getStatement: Statement = {
    val balanceInformation = BalanceInformation(balance, limit, LocalDateTime.now())
    val lastTransactions = transactions.take(10)
    Statement(balanceInformation, lastTransactions)
  }

  def balance: Int = {
    balanceSnapshot + balance(transactions)
  }

  def balance(transactions: List[Transaction]): Int = {
    transactions.foldRight(0)((t: Transaction, acc: Int) => t match {
      case Credit(value, _, _) => acc + value
      case Debit(value, _, _) => acc - value
    })
  }
}