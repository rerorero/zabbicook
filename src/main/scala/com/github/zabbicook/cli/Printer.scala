package com.github.zabbicook.cli

trait Printer {
  def out(msg: String): Unit
  def error(msg: String): Unit
}

object Printer {
  val default = new Printer {
    override def out(msg: String): Unit = System.out.println(msg)
    override def error(msg: String): Unit = System.err.println(msg)
  }
}
