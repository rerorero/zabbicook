package com.github.zabbicook.test

import com.github.zabbicook.api.{ZabbixApi, ZabbixApiConf}
import com.github.zabbicook.entity.Version
import com.github.zabbicook.operation.Ops

trait TestConfig {
  lazy val cachedOps: Map[Version, Ops] = TestConfig.apiConfs.mapValues(conf => new Ops(new ZabbixApi(conf)))

  sys.addShutdownHook{
    cachedOps.foreach(_._2.close())
  }

  def withTestOp(f: (Ops, Version) => Unit): Unit = {
    cachedOps.foreach { case (version, ops) => f(ops, version) }
  }

  def withTestApiConf(f: (ZabbixApiConf, Version) => Unit): Unit = {
    TestConfig.apiConfs.foreach { case (version, conf) => f(conf, version) }
  }
}

object TestConfig {
  val apiConfs: Map[Version, ZabbixApiConf] = Map(
    Version(3,0,5) -> ZabbixApiConf(
      apiPath = "http://localhost:8080/api_jsonrpc.php",
      authUser = "Admin",
      authPass = "zabbix"
    ),
    Version(3,2,1) -> ZabbixApiConf(
      apiPath = "http://localhost:8081/api_jsonrpc.php",
      authUser = "Admin",
      authPass = "zabbix"
    )
  )
}
