package com.github.zabbicook.operation

import com.github.zabbicook.api.{ZabbixApi, ZabbixApiConf}

class Ops(api: ZabbixApi) {
  val user =  new UserOp(api)
  val userGroup = new UserGroupOp(api)
  val hostGroup = new HostGroupOp(api)
  val template = new TemplateOp(api)
  val item = new ItemOp(api)

  def close(): Unit = api.close()
}

object Ops {
  def create(apiConf: ZabbixApiConf): Ops = {
    val api = new ZabbixApi(apiConf)
    new Ops(api)
  }
}
