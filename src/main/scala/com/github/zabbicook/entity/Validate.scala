package com.github.zabbicook.entity

import com.github.zabbicook.api.Version
import com.github.zabbicook.operation.Ops

import scala.concurrent.Future


case class ValidateError(messages: Seq[String]) extends Exception(
  s"Validation Error: ${messages.length} errors." + System.lineSeparator() +
    messages.mkString(System.lineSeparator())
) {
  def append(error: ValidateError): ValidateError = copy(messages ++ error.messages)
}

object ValidateError {
  def of(msg: String) = ValidateError(Seq(msg))
}

trait Validate {
  def validate(op: Ops, version: Version): Future[Unit]
}
