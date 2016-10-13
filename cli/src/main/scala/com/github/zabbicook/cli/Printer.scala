package com.github.zabbicook.cli

trait Printer {
  def printMsg(msg: String): Unit
  def errorMsg(msg: String): Unit
}

object Printer {
  val default = new Printer {
    override def printMsg(msg: String): Unit = System.out.println(msg)
    override def errorMsg(msg: String): Unit = System.err.println(msg)
  }
}
