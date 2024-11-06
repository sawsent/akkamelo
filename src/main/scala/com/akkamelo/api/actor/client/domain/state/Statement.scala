package com.akkamelo.api.actor.client.domain.state

import java.time.LocalDateTime

case class Statement(balanceInformation: BalanceInformation, lastTransactions: List[Transaction])


case class BalanceInformation(balance: Int, limit: Int, timestamp: LocalDateTime) {
  override def equals(obj: Any): Boolean = obj match {
    case b: BalanceInformation => b.balance == balance && b.limit == limit
    case _ => false
  }
}
