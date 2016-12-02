package com.github.zabbicook.test

import java.time.Duration

import com.github.zabbicook.api.{Version, ZabbixApi, ZabbixApiConf}
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
  // to avoid 'DBEXECUTE_ERROR' on travis.ci (experimental)
  val sleep: Duration = Duration.ofMillis(System.getProperty("sleep", "0").toInt)
  println(s"sleep: ${sleep.toMillis} msec")

  val apiConfs: Map[Version, ZabbixApiConf] = Map(
    Version(3,0,5) -> ZabbixApiConf(
      apiPath = "http://localhost:8080/api_jsonrpc.php",
      authUser = "Admin",
      authPass = "zabbix",
      interval = sleep,
      concurrency = false
    ),
    Version(3,2,1) -> ZabbixApiConf(
      apiPath = "http://localhost:8081/api_jsonrpc.php",
      authUser = "Admin",
      authPass = "zabbix",
      interval = sleep,
      concurrency = false
    )
  )
}
