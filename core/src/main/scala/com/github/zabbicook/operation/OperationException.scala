package com.github.zabbicook.operation

class OperationException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

case class NoSuchEntityException(msg: String, cause: Throwable = null) extends OperationException(msg, cause)
