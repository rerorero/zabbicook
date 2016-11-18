package com.github.zabbicook.operation

class OperationException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

case class NoSuchEntityException(msg: String, cause: Throwable = null) extends OperationException(msg, cause)

case class BadReferenceException(msg: String, cause: Throwable = null) extends OperationException(msg, cause)

case class ItemKeyDuplicated(msg: String, cause: Throwable = null) extends OperationException(msg, cause)

case class NoAvailableEntities(msg: String, cause: Throwable = null) extends OperationException(msg, cause)

case class UnsupportedOperation(msg: String, cause: Throwable = null) extends OperationException(msg, cause)
