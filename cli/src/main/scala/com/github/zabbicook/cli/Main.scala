package com.github.zabbicook.cli

import com.github.zabbicook.cli.RunResult.RunSuccess

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Main(printer: Printer) {
  def run(args: Array[String]): Future[Int] = {
    Arguments.parser.parse(args, Arguments()) match {
      case Some(conf) if conf.showVersion =>
        Future {
          printer.printMsg(BuildInfo.version)
          0
        }
      case Some(conf) =>
        val runner = new Runner(conf, printer)
        runner.run().map {
          case _: RunSuccess => 0
          case e =>
            printer.errorMsg("Failed.")
            1
        }
      case None =>
        printer.errorMsg("Failed in parsing arguments.")
        Future.successful(1)
    }
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      Arguments.parser.showUsage()
    } else {
      new Main(Printer.default).run(args).map(rc => sys.exit(rc))
    }
  }
}

