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
class ItemOp(api: ZabbixApi, templateOp: TemplateOp) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  /**
    * get items inherited from parent templates
    */
  def getInheritedItems(hostId: StoredId): Future[Seq[Item[Stored]]] = findByHostId(hostId, inherited = true)

  /**
    * get items without inherited items
    */
  def getBelongingItems(hostId: StoredId): Future[Seq[Item[Stored]]] = findByHostId(hostId, inherited = false)

  private[this] def findByHostId(hostId: StoredId, inherited: Boolean): Future[Seq[Item[Stored]]] = {
    val params = Json.obj()
      .prop("hostids" -> hostId)
      .prop("inherited" -> inherited)
    api.requestAs[Seq[Item[Stored]]]("item.get", params)
  }

  def findByHostId(hostId: StoredId): Future[Seq[Item[Stored]]] = {
    val params = Json.obj().prop("hostids" -> hostId)
    api.requestAs[Seq[Item[Stored]]]("item.get", params)
  }

  def create(hostId: StoredId, item: Item[NotStored]): Future[Report] = {
    val newItem = item.setHost(hostId)
    val param = Json.toJson(newItem)
    api.requestSingleId("item.create", param, "itemids")
      .map(id => Report.created(newItem.toStored(id)))
  }

  def update(itemId: StoredId, hostId: StoredId, item: Item[NotStored]): Future[Report] = {
    val param = Json.toJson(item.toStored(itemId))
    api.requestSingleId("item.update", param, "itemids")
      .map(id => Report.updated(item.toStored(id)))
  }

  def delete(items: Seq[Item[Stored]]): Future[Report] = {
    // deleteEntities(api, items, "item.delete", "templateids")
    // TODO Why does zabbix api respond which schema differs from other 'delete' methods...?
    // I expect that delete method returns array of ids, but actual is like this:
    //   {"jsonrpc":"2.0","result":{"itemids":{"0":26679,"1":26681,"26680":"26680","26682":"26682"}},"id":951237945}
    if (items.isEmpty) {
      Future.successful(Report.empty())
    } else {
      val param = Json.toJson(items.map(_.getStoredId))
      api.requestAs[JsObject]("item.delete", param)
        .map(_ => Report.deleted(items))
    }
  }

  def presentWithTemplate(template: String, items: Seq[Item[NotStored]]): Future[Report] = {
    def checkDup(inherits: Seq[Item[Stored]]): Unit = {
      val duplicated = items.groupBy(_.`key_`).find(_._2.length > 1)
      duplicated.foreach(dup => throw ItemKeyDuplicated(
        s"Keys of items duplicated. key=${dup._2.headOption.map(_.`key_`).getOrElse("")} items=${dup._2.map(_.name).mkString(",")}"
      ))
      items.foreach { item =>
        inherits.find(_.`key_` == item.`key_`).foreach { i =>
          throw ItemKeyDuplicated(s"the key '${i.`key_`}' of '${item.name}' is duplicated with the item '${i.name}' which belongs to parent templates")
        }
      }
    }

    def createOrUpdate(hostId: StoredId, belongs: Seq[Item[Stored]]): Future[Report] = {
      traverseOperations(items) { item =>
        belongs.find(_.`key_` == item.`key_`) match {
          case Some(stored) =>
            if (stored.shouldBeUpdated(item))
              update(stored.getStoredId, hostId, item)
            else
              Future.successful(Report.empty())
          case None =>
            create(hostId, item)
        }
      }
    }

    for {
      Seq(ts) <- templateOp.findByHostnamesAbsolutely(Seq(template))
      hostId = ts.template.getStoredId
      inheritedItems <- getInheritedItems(ts.template.getStoredId)
      _ = checkDup(inheritedItems)
      belongingItems <- getBelongingItems(ts.template.getStoredId)
      updated <- createOrUpdate(hostId, belongingItems)
      deleted <- delete(belongingItems.filter(i => !items.exists(_.`key_` == i.`key_`)))
    } yield {
      updated + deleted
    }
  }

  def presentWithTemplate(items: Map[String, Seq[Item[NotStored]]]): Future[Report] = {
    traverseOperations(items.toSeq) { case (template, _items) =>
      showStartInfo(logger, _items.length, s"items of template '$template'").flatMap(_ =>
        presentWithTemplate(template, _items)
      )
    }
  }

  /**
    * Delete items in the specified template
    * Ignores if the template does not exist
    * @param items Paris of template name and item names
    * @return
    */
  def absentWithTemplate(items: Map[String, Seq[String]]): Future[Report] = {
    for {
      templates <- templateOp.findByHostnames(items.keys.toSeq)
      report <- traverseOperations(templates) { t =>
        getBelongingItems(t.template.getStoredId).flatMap { stored =>
          delete(stored.filter(s => items(t.template.host).contains(s)))
        }
      }
    } yield report
  }
}
