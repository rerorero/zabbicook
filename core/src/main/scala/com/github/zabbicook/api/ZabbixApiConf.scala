package com.github.zabbicook.api

import com.ning.http.client.AsyncHttpClientConfig
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

/**
  * @param apiPath zabbix API URL ex) http://company.com/zabbix/api_jsonrpc.php
  * @param jsonrpc jsonrpc version
  * @param executionContext execution context
  */
case class ZabbixApiConf(
  apiPath: String,
  jsonrpc: String = "2.0",
  authUser: String,
  authPass: String,
  executionContext: ExecutionContext = ExecutionContext.global
) {
  val jsonRpcUrl = {
    val php = "api_jsonrpc.php"
    if (apiPath.endsWith(php)) {
      apiPath
    } else if (apiPath.endsWith("/")) {
      apiPath + php
    } else {
      apiPath + "/" + php
    }
  }
  def httpClientConfig: AsyncHttpClientConfig = {
    new AsyncHttpClientConfig.Builder()
      .setAcceptAnyCertificate(true)
      .setRequestTimeout(30000)
      .build()
  }
}

object ZabbixApiConf {
  def load(config: Config): ZabbixApiConf = {
    ZabbixApiConf(
      apiPath = config.getString("endpoint.url"),
      authUser = config.getString("endpoint.auth.user"),
      authPass = config.getString("endpoint.auth.password")
    )
  }
}
