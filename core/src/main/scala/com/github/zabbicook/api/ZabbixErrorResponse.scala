package com.github.zabbicook.api

import play.api.libs.json.{Format, Json}

/**
  * @see https://www.zabbix.com/documentation/3.0/manual/api#error_handling
  */
case class ZabbixErrorResponse(
  code: String,
  message: String,
  data: String
)

object ZabbixErrorResponse {
  implicit val formt: Format[ZabbixErrorResponse] = Json.format[ZabbixErrorResponse]
}

