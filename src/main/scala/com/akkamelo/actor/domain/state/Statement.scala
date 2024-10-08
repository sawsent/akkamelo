package com.akkamelo.actor.domain.state

import java.time.LocalDateTime

case class Statement(balanceInformation: BalanceInformation, lastTransactions: List[Transaction])


case class BalanceInformation(balance: Int, limit: Int, timestamp: LocalDateTime)