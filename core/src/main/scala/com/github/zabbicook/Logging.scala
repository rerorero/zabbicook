package com.github.zabbicook

import com.github.zabbicook.LoggerName.Api
import com.typesafe.scalalogging
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap

sealed trait LoggerName

object LoggerName {
  case object Api extends LoggerName
}

trait Logging {
  def loggerOf(name: LoggerName): Logger = Logging.all(name)
}

object Logging {
  val all = TrieMap[LoggerName, Logger](
    Api -> scalalogging.Logger(LoggerFactory.getLogger("api"))
  )
}
