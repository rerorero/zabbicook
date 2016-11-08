package com.github.zabbicook.api

import play.api.libs.json.{Format, Json}

/**
  * @see https://www.zabbix.com/documentation/3.0/manual/api#error_handling
  */
case class ZabbixErrorResponse(
  code: Long,
  message: String,
  data: String
) {
  def getMessage: String = s"$message - $data"
}

object ZabbixErrorResponse {
  implicit val formt: Format[ZabbixErrorResponse] = Json.format[ZabbixErrorResponse]
}

