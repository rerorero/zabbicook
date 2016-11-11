package com.github.zabbicook.cli

import java.io.File

import com.github.zabbicook.hocon.HoconError
import com.github.zabbicook.operation.Report
import play.api.libs.json.{Json, Writes}

sealed trait RunResult {
  def asString: String
  def isSuccess: Boolean
}

object RunResult {

  case class RunSuccess(report: Report) extends RunResult {
    override def asString: String = {
      report.toStringSeq().mkString(System.lineSeparator()) +
        System.lineSeparator() +
        s"Succeed: Total changes = ${report.count}"
    }
    override val isSuccess: Boolean = true
  }

  sealed trait RunError extends RunResult {
    override val isSuccess: Boolean = false
  }

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

  case class ArgumentError(msg: String) extends RunError {
    override def asString: String = msg
  }

  implicit val writesJson: Writes[RunResult] = Writes[RunResult] {
    case RunSuccess(r) =>
      Json.obj("result" -> "success", "report" -> Json.toJson(r))
    case e: RunError =>
      Json.obj("result" -> "fail", "error" -> e.asString)
  }
}

