package com.github.zabbicook.operation

import com.github.zabbicook.api.{ZabbixApi, ZabbixApiConf}

class OperationSet(api: ZabbixApi) {
  val user =  new UserOp(api)
  val userGroup = new UserGroupOp(api)
  val hostGroup = new HostGroupOp(api)

  def close(): Unit = api.close()
}

object OperationSet {
  def create(apiConf: ZabbixApiConf): OperationSet = {
    val api = new ZabbixApi(apiConf)
    new OperationSet(api)
  }
}
