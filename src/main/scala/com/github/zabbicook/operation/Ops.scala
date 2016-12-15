package com.github.zabbicook.operation

import com.github.zabbicook.api.{Version, ZabbixApi, ZabbixApiConf}

import scala.concurrent.Future

class Ops(api: ZabbixApi) {
  val mediaType = new MediaTypeOp(api)
  val hostGroup = new HostGroupOp(api)
  val userGroup = new UserGroupOp(api, hostGroup)
  val user =  new UserOp(api, userGroup, mediaType)
  val action = new ActionOp(api, userGroup, user, mediaType)
  val template = new TemplateOp(api, hostGroup)
  val screen = new ScreenOp(api)
  val templateScreen = new TemplateScreenOp(api, template)
  val item = new ItemOp(api, template)
  val trigger = new TriggerOp(api, template)
  val graph = new GraphOp(api, template, item)
  val host = new HostOp(api, hostGroup, template)
  val screenItem = new ScreenItemOp(api, template, screen, templateScreen, graph, item, hostGroup, host)

  def close(): Unit = api.close()

  def getVersion(): Future[Version] = api.getVersion()
}

object Ops {
  def create(apiConf: ZabbixApiConf): Ops = {
    val api = new ZabbixApi(apiConf)
    new Ops(api)
  }
}
