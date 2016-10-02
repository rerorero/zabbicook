package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.item.Item
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * item api
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/item
  */
class ItemOp(api: ZabbixApi) extends OperationHelper with Logging {

  private[this] val templateOp = new TemplateOp(api)

  /**
    * get items inherited from parent templates
    */
  def getInheritedItems(hostId: StoredId): Future[Seq[Item[Stored]]] = findByHostId(hostId, inherited = true)

  /**
    * get items without inherited items
    */
  def getBelongingItems(hostId: StoredId): Future[Seq[Item[Stored]]] = findByHostId(hostId, inherited = false)

  def findByHostId(hostId: StoredId, inherited: Boolean): Future[Seq[Item[Stored]]] = {
    val params = Json.obj()
      .prop("hostid" -> hostId)
      .prop("inherited" -> inherited)
    api.requestAs[Seq[Item[Stored]]]("item.get", params)
  }

  def create(item: Item[NotStored]): Future[(StoredId, Report)] = {
    val param = Json.toJson(item)
    api.requestSingleId("item.create", param, "itemids")
      .map(id => (id, Report.created(item.toStored(id))))
  }

  def update(itemId: StoredId, item: Item[NotStored]): Future[(StoredId, Report)] = {
    val param = Json.toJson(item.toStored(itemId))
    api.requestSingleId("item.update", param, "itemids")
      .map(id => (id, Report.updated(item.toStored(id))))
  }

  def delete(items: Seq[Item[Stored]]): Future[(Seq[StoredId], Report)] = {
    deleteEntities(api, items, "item.delete", "templateids")
  }

  def presentWithTemplate(template: String, items: Seq[Item[NotStored]]): Future[(Seq[StoredId], Report)] = {
    def checkDup(inherits: Seq[Item[Stored]]): Unit = {
      val duplicated = items.groupBy(_.`key_`).find(_._2.length > 1)
      duplicated.foreach(dup => throw ItemKeyDuplicated(
        s"Keys of items duplicated. key=${dup._2.headOption.map(_.`key_`).getOrElse("")} items=${dup._2.map(_.name).mkString(",")}"
      ))
      items.foreach { item =>
        inherits.find(_.`key_` == item.`key_`).foreach { i =>
          throw ItemKeyDuplicated(s"the key '${i.`key_`}' of '${item.name}' duplicated with items which belong parent templates")
        }
      }
    }

    for {
      Seq(ts) <- templateOp.findByHostnamesAbsolutely(Seq(template))
      inheritedItems <- getInheritedItems(ts.template.getStoredId)
      belongingItems <- getBelongingItems(ts.template.getStoredId)
      _ = checkDup(inheritedItems)
      deleted <- delete(belongingItems.filter(i => !items.exists(_.`key_` == i.`key_`)))
//      _ <- {
//        items.foldLeft((Seq.empty[StoredId], Report)) { (acc, item) =>
//          val r = belongingItems.find(_.`key_` == item.`key_`) match {
//            case Some(stored) =>
//              if (stored.shouldBeUpdated(item))
//                update(stored.getStoredId, item)
//              else
//                Future((Seq(), Report.empty()))
//            case None =>
//              create(item)
//          }
//        }
    } yield  {
      ???
    }
  }
}
