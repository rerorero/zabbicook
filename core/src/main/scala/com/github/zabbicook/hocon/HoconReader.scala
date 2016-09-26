package com.github.zabbicook.hocon

import java.io.File

import com.typesafe.config.{Config, ConfigException, ConfigFactory}

object HoconReader {
  def read[T](v: => Config)(implicit reader: HoconReads[T]): HoconResult[T] = {
    try {
      reader.read(v)
    } catch {
      case e: ConfigException => HoconError.from(e)
    }
  }

  def read[T](file: File)(implicit reader: HoconReads[T]): HoconResult[T] = read(ConfigFactory.parseFile(file))

  def read[T](s: String)(implicit reader: HoconReads[T]): HoconResult[T] = read(ConfigFactory.parseString(s))
}
