package com.github.zabbicook.api

import com.ning.http.client.AsyncHttpClientConfig
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

/**
  * @param url zabbix API URL ex) http://company.com/zabbix/api_jsonrpc.php
  * @param jsonrpc jsonrpc version
  * @param httpClientConfig HttpClientConfig
  * @param executionContext execution context
  */
case class ZabbixApiConf(
  url: String,
  jsonrpc: String = "2.0",
  httpClientConfig: AsyncHttpClientConfig = ZabbixApiConf.defaultHttpClientConfig,
  authUser: String,
  authPass: String,
  executionContext: ExecutionContext = ExecutionContext.global
)

object ZabbixApiConf {
  val defaultHttpClientConfig =
    new AsyncHttpClientConfig.Builder()
      .setAcceptAnyCertificate(true)
      .setRequestTimeout(30000)
      .build()

  def load(config: Config): ZabbixApiConf = {
    ZabbixApiConf(
      url = config.getString("endpoint.url"),
      authUser = config.getString("endpoint.auth.user"),
      authPass = config.getString("endpoint.auth.password")
    )
  }
}
