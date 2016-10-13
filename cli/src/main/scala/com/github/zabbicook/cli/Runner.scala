package com.github.zabbicook.cli

import java.io.File

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApiConf
import com.github.zabbicook.chef.Chef
import com.github.zabbicook.cli.RunResult.{FileNotFound, NoInputSpecified, OtherError, ParseError, RunSuccess}
import com.github.zabbicook.hocon.{HoconError, HoconReader, HoconSuccess}
import com.github.zabbicook.operation.{Ops, Report}
import com.github.zabbicook.recipe.Recipe
import play.api.libs.json.{Json, Writes}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import com.github.zabbicook.hocon.HoconReadsCompanion._
import com.github.zabbicook.hocon.HoconReads.option

private[cli] sealed trait RunResult {
  def asString: String
}

private[cli] object RunResult {

  case class RunSuccess(report: Report) extends RunResult {
    override def asString: String = {
      report.toStringSeq().mkString(System.lineSeparator()) +
        System.lineSeparator() +
        s"Succeed: Total changes = ${report.count}"
    }
  }

  sealed trait RunError extends RunResult

  case class OtherError(e: Throwable) extends RunError {
    override def asString: String = e.getMessage()
  }

  case class ParseError(e: HoconError) extends RunError {
    override def asString: String = e.toString()
  }

  case object NoInputSpecified extends RunError {
    override def asString: String = "No input files specified. Try with --file option."
  }

  case class FileNotFound(file: File) extends RunError {
    override def asString: String = s"No such file: ${file.getAbsolutePath}"
  }

  implicit val writesJson: Writes[RunResult] = Writes[RunResult] {
    case RunSuccess(r) =>
      Json.obj("result" -> "success", "report" -> Json.toJson(r))
    case e: RunError =>
      Json.obj("result" -> "fail", "error" -> e.asString)
  }
}

class Runner(conf: Arguments, printer: Printer) extends Logging {

  val apiConf = new ZabbixApiConf(
    apiPath = conf.url.toString,
    authUser = conf.adminUser,
    authPass = conf.adminPass
  )

  def run(): Future[RunResult] = {
    val operationSet = Ops.create(apiConf)
    val chef = new Chef(operationSet)

    configureLogging()
    presentRecipe(chef).map { result =>
      outFormatted(result)
      result
    }.andThen {
      case _ => operationSet.close()
    }
  }

  private[this] def configureLogging(): Unit = {
    if (conf.isDebug) {
      // set debug level anyway if debug enabled
      Logging.debugging()
    } else if (conf.isJson) {
      // When formatted in Json, disables logging to be able to parse output as Json when failure cases.
      Logging.silent()
    } else {
      Logging.info()
    }
  }

  private[this] def presentRecipe(chef: Chef): Future[RunResult] = {
    conf.input match {
      case Some(f) if (!f.exists() || !f.isFile()) => Future.successful(FileNotFound(f))
      case Some(f) =>
        (HoconReader.read[Recipe](f, Recipe.optional("root")) match {
          case HoconSuccess(recipe) =>
            chef.present(recipe).map(RunSuccess)
          case e: HoconError =>
            Future.successful(ParseError(e))
        }).recover {
          case NonFatal(e) => OtherError(e)
        }
      case None => Future.successful(NoInputSpecified)
    }
  }

  private[this] def outFormatted(result: RunResult): Unit = {
    def print(msg: String): Unit = result match {
      case _: RunSuccess => printer.out(msg)
      case _ => printer.error(msg)
    }
    if (conf.isJson) {
      print(Json.prettyPrint(Json.toJson(result)))
    } else {
      print(result.asString)
    }
  }
}
