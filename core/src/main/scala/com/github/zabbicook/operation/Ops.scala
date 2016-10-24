package com.github.zabbicook.operation

import com.github.zabbicook.api.{ZabbixApi, ZabbixApiConf}

class Ops(api: ZabbixApi) {
  val mediaType = new MediaTypeOp(api)
  val hostGroup = new HostGroupOp(api)
  val userGroup = new UserGroupOp(api, hostGroup)
  val user =  new UserOp(api, userGroup, mediaType)
  val template = new TemplateOp(api, hostGroup)
  val item = new ItemOp(api, template)
  val graph = new GraphOp(api, template, item)
  val host = new HostOp(api, hostGroup, template)

  def close(): Unit = api.close()
}

object Ops {
  def create(apiConf: ZabbixApiConf): Ops = {
    val api = new ZabbixApi(apiConf)
    new Ops(api)
  }
}
