package com.github.zabbicook.cli

import com.github.zabbicook.cli.RunResult.RunSuccess
import shapeless.BuildInfo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Main(printer: Printer) {
  def run(args: Array[String]): Future[Int] = {
    Arguments.parser.parse(args, Arguments()) match {
      case Some(conf) if conf.showVersion =>
        printer.out(BuildInfo.version)
        succeed()

      case Some(conf) if conf.setPassword =>
        conf.newPassword match {
          case Some(newPass) =>
            val runner = new Runner(conf, printer)
            runner.changePassword(conf.user, newPass, conf.pass).flatMap {
              case _: RunSuccess => succeed()
              case e => failed(3, "Failed.")
            }
          case None =>
            failed(1, "Failed in parsing arguments. Changing password requires --user, --pass, and --new-pass options.")
        }

      case Some(conf) =>
        // zabbicook main sequence.
        val runner = new Runner(conf, printer)
        runner.run().flatMap {
          case _: RunSuccess => succeed()
          case e => failed(1, "Failed.")
        }

      case None =>
        failed(2, "Failed in parsing arguments.")
    }
  }

  private[this] def succeed() = Future.successful(0)

  private[this] def failed(code: Int, error: String): Future[Int] = {
    printer.error(error)
    Future.successful(code)
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

