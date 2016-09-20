package com.github.zabbicook.test

import com.github.zabbicook.api.{ZabbixApi, ZabbixApiConf}
import com.typesafe.config.ConfigFactory

trait TestConfig {
  lazy val apiConf = TestConfig.apiConf
  lazy val cachedApi = new ZabbixApi(apiConf)
}

object TestConfig {
  lazy val apiConf = ZabbixApiConf.load(ConfigFactory.load())
}
