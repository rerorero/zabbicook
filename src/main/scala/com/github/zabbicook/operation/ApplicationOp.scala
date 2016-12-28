package com.github.zabbicook.operation

import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.item.Application
import play.api.libs.json.{JsObject, Json}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class ApplicationOp(api: ZabbixApi) extends OperationHelper {

  def getInherited(hostid: StoredId): Future[Seq[Application[Stored]]] = findByHost(hostid, inherited = true)

  def getBelonging(hostid: StoredId): Future[Seq[Application[Stored]]] = findByHost(hostid, inherited = false)

  private[this] def findByHost(hostid: StoredId, inherited: Boolean): Future[Seq[Application[Stored]]] = {
    val params = Json.obj()
      .outExtend()
      .prop("hostids" -> hostid)
      .prop("inherited" -> inherited)
    api.requestAs[Seq[Application[Stored]]]("application.get", params)
  }

  def findByHostId(hostid: StoredId): Future[Seq[Application[Stored]]] = {
    val params = Json.obj()
      .outExtend()
      .prop("hostids" -> hostid)
    api.requestAs[Seq[Application[Stored]]]("application.get", params)
  }

  def create(hostid: StoredId, app: Application[NotStored]): Future[Report] = {
    val params = Json.toJson(app.setHostId(hostid)).as[JsObject]
    api.requestSingleId("application.create", params, "applicationids")
      .map(id => Report.created(app.toStored(id)))
  }

  def createAll(hostid: StoredId, apps: Seq[Application[NotStored]]): Future[Report] = {
    traverseOperations(apps)(create(hostid, _))
  }

  def delete(apps: Seq[Application[Stored]]): Future[Report] = {
    deleteEntities(api, apps, "application.delete", "applicationids")
  }
}
