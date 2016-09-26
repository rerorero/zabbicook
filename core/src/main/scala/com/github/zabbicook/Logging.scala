package com.github.zabbicook

import com.typesafe.scalalogging
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import org.slf4j.{Logger => SlfLogger}
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}

trait Logging {
  def defaultLogger: Logger = Logging.default
}

object Logging {
  val default = scalalogging.Logger(LoggerFactory.getLogger("default"))

  def silent(): Unit = setLogLevel(Level.OFF)

  def debugging(): Unit = setLogLevel(Level.DEBUG)

  private[this] def setLogLevel(level: Level): Unit = {
    val root = LoggerFactory.getLogger(SlfLogger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger]
    root.setLevel(level)
  }
}
