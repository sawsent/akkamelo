package com.akkamelo.core.logging

import org.apache.logging.log4j.{LogManager, Logger}

trait BaseLogging {
  protected lazy val logger: Logger = LogManager.getLogger(getClass)
}
