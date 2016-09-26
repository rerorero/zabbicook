package com.github.zabbicook.cli

trait Printer {
  def print(msg: String): Unit
}

object Printer {
  val stdOutPrinter = new Printer {
    override def print(msg: String): Unit = println(msg)
  }
}
