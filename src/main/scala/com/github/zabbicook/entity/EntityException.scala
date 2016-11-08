package com.github.zabbicook.entity

case class EntityException(msg: String, cause: Throwable = null) extends Exception(msg, cause)
