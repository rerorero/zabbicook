package com.github.zabbicook.api

import java.time.Duration

import com.ning.http.client.AsyncHttpClientConfig

import scala.concurrent.ExecutionContext

/**
  * @param apiPath zabbix API URL ex) http://company.com/zabbix/api_jsonrpc.php
  * @param jsonrpc jsonrpc version
  * @param authUser admin user
  * @param authPass admin password
  * @param interval interval of calling api
  * @param concurrency flag of concurrency
  * @param executionContext execution context
  */
case class ZabbixApiConf(
  apiPath: String,
  jsonrpc: String = "2.0",
  authUser: String,
  authPass: String,
  interval: Duration,
  concurrency: Boolean,
  executionContext: ExecutionContext = ExecutionContext.global,
  timeout: Duration = Duration.ofSeconds(20)
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
      .setRequestTimeout(timeout.toMillis.toInt)
      .build()
  }
}
