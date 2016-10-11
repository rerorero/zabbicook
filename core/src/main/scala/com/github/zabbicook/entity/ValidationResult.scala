package com.github.zabbicook.entity

sealed trait ValidationResult {
  def valid: Boolean
}

object ValidationResult {
  case object Valid extends ValidationResult { def valid = true }

  sealed trait Invalid extends ValidationResult {
    def valid = false
    def detail: String
  }

  case class NotAcceptable(acceptable: Set[String] = Set()) extends Invalid {
    def detail = {
      val acceptables = if (acceptable.isEmpty) "" else {
        s"(Acceptable values are: ${acceptable.map(s => s"'$s'").mkString(", ")})"
      }
      "Not acceptable value" + acceptables
    }
  }

  case class Nul(acceptable: Set[String] = Set()) extends Invalid {
    def detail = {
      val acceptables = if (acceptable.isEmpty) "" else {
        s"(Acceptable values are: ${acceptable.map(s => s"'$s'").mkString(", ")})"
      }
      "null cannot be used. " + acceptables
    }
  }

  //case class outOfRange(msg: String) extends ValidationResult(msg)
}
