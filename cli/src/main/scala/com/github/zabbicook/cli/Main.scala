package com.github.zabbicook.cli

import com.github.zabbicook.cli.RunResult.RunSuccess

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Main(printer: Printer) {
  def run(args: Array[String]): Future[Int] = {
    Configurations.parser.parse(args, Configurations()) match {
      case Some(conf) =>
        val runner = new Runner(conf, printer)
        runner.run().map {
          case _: RunSuccess => 0
          case e => 1
        }
      case None =>
        println("Failed in parsing arguments.")
        Future.successful(1)
    }
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    new Main(Printer.stdOutPrinter).run(args).map(rc => sys.exit(rc))
  }
}

