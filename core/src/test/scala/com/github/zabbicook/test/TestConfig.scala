package com.github.zabbicook.test

import com.github.zabbicook.api.{ZabbixApi, ZabbixApiConf}
import com.github.zabbicook.operation.Ops

trait TestConfig {
  lazy val cachedApi30 = new ZabbixApi(TestConfig.apiConf30)

  lazy val cachedApi32 = new ZabbixApi(TestConfig.apiConf32)

  sys.addShutdownHook{
    cachedApi30.close()
    cachedApi32.close()
  }

  def withTestOp(f: (Ops, String) => Unit): Unit = {
    Seq(
      (new Ops(cachedApi30), "Ver3.0.x: "),
      (new Ops(cachedApi32), "Ver3.2.x: ")
    ).foreach(tpl => f(tpl._1, tpl._2))
  }

  def withTestApiConf(f: (ZabbixApiConf, String) => Unit): Unit = {
    Seq(
      (TestConfig.apiConf30, "Ver3.0.x: "),
      (TestConfig.apiConf32, "Ver3.2.x: ")
    ).foreach(tpl => f(tpl._1, tpl._2))
  }
}

object TestConfig {
  lazy val apiConf30 = ZabbixApiConf(
    apiPath = "http://localhost:8080/api_jsonrpc.php",
    authUser = "Admin",
    authPass = "zabbix"
  )
  lazy val apiConf32 = ZabbixApiConf(
    apiPath = "http://localhost:8081/api_jsonrpc.php",
    authUser = "Admin",
    authPass = "zabbix"
  )
}
