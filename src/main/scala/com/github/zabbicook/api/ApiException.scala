package com.github.zabbicook.api

class ApiException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

case class ErrorResponseException(method: String, response: ZabbixErrorResponse, additional: String, cause: Throwable = null)
  extends ApiException(s"${method} responds error: ${response.getMessage} $additional", cause)

case class NotSingleException(method: String, size: Int, cause: Throwable = null)
  extends ApiException(s"${method} responds ${size} values", cause)

