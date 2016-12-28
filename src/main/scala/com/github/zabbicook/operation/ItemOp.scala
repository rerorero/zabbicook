package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityException
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.item.{Application, Item}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * item api
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/item
  */
class ItemOp(api: ZabbixApi, templateOp: TemplateOp, applicationOp: ApplicationOp) extends OperationHelper with Logging {

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
      .prop("selectApplications" -> "extend")
    api.requestAs[Seq[Item[Stored]]]("item.get", params)
  }

  def findByNameAndHost(name: String, hostId: StoredId): Future[Option[Item[Stored]]] = {
    val params = Json.obj()
      .prop("hostids" -> hostId)
      .filter("name" -> name)
      .prop("selectApplications" -> "extend")
    api.requestSingleAs[Item[Stored]]("item.get", params)
  }

  def findByNameAndHostAbsolutely(name: String, hostId: StoredId): Future[Item[Stored]] = {
    findByNameAndHost(name, hostId).map(_.getOrElse(throw NoSuchEntityException(s"No such template item: $name")))
  }

  def create(hostId: StoredId, item: Item[NotStored], applicationIds: Seq[StoredId]): Future[Report] = {
    val newItem = item.setHost(hostId)
    val param = Json.toJson(newItem).as[JsObject]
      .updated("applications" -> applicationIds)
    api.requestSingleId("item.create", param, "itemids")
      .map(id => Report.created(newItem.toStored(id)))
  }

  def update(itemId: StoredId, hostId: StoredId, item: Item[NotStored], applicationIds: Seq[StoredId]): Future[Report] = {
    val param = Json.toJson(item.toStored(itemId)).as[JsObject]
      .updated("applications" -> applicationIds)
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

  def presentWithTemplate(template: String, applications: Seq[String], items: Seq[Item[NotStored]]): Future[Report] = {
    def checkDup(inherits: Seq[Item[Stored]]): Unit = {
      val duplicated = items.groupBy(_.`key_`).find(_._2.length > 1)
      duplicated.foreach(dup => throw EntityDuplicated(
        s"Key of items has duplicate. key=${dup._2.headOption.map(_.`key_`).getOrElse("")} items=${dup._2.map(_.name).mkString(",")}"
      ))
      items.foreach { item =>
        inherits.find(_.`key_` == item.`key_`).foreach { i =>
          throw EntityDuplicated(s"Key '${i.`key_`}' of '${item.name}' are duplicated with the item '${i.name}' which belongs to parent templates")
        }
      }
    }

    def applicationsOf(hostId: StoredId, item: Item[NotStored]): Future[Seq[Application[Stored]]] = {
      val appNames = item.applicationNames.getOrElse(Seq())
      appNames match {
        case Nil =>
          Future.successful(Seq())
        case _ =>
          applicationOp.findByHostId(hostId).map { storedApps =>
            appNames.filter(!storedApps.map(_.name).contains(_)) match {
              case Nil =>
                storedApps.filter(a => appNames.contains(a.name))
              case notExist =>
                val list = notExist.map(s => s"'${s}'").mkString(",")
                throw EntityException(s"Application names $list (of item '${item.name}') do not exist in the template '$template'.")
            }
          }
      }
    }

    def createOrUpdate(hostId: StoredId, belongs: Seq[Item[Stored]]): Future[Report] = {
      traverseOperations(items) { item =>
        belongs.find(_.`key_` == item.`key_`) match {
          case Some(stored) =>
            if (stored.shouldBeUpdated(item)) {
              for {
                apps <- applicationsOf(hostId, item)
                report <- update(stored.getStoredId, hostId, item, apps.map(_.getStoredId))
              } yield report
            } else {
              Future.successful(Report.empty())
            }
          case None =>
            for {
              apps <- applicationsOf(hostId, item)
              report <- create(hostId, item, apps.map(_.getStoredId))
            } yield report
        }
      }
    }

    for {
      Seq(ts) <- templateOp.findByHostnamesAbsolutely(Seq(template))
      hostId = ts.template.getStoredId
      _ <- showStartInfo(logger, items.length, s"items of template '$template'")

      // creates applications at first
      belongingApps <- applicationOp.getBelonging(hostId)
      delApps = belongingApps.map(_.name).diff(applications)
        .map(n => belongingApps.find(_.name == n))
        .flatten
      newApps = applications.diff(belongingApps.map(_.name))
      appCreated <- applicationOp.createAll(hostId, newApps.map(Application.toNotStored))

      // present items
      inheritedItems <- getInheritedItems(ts.template.getStoredId)
      _ = checkDup(inheritedItems)
      belongingItems <- getBelongingItems(ts.template.getStoredId)
      updated <- createOrUpdate(hostId, belongingItems)
      deleted <- delete(belongingItems.filter(i => !items.exists(_.`key_` == i.`key_`)))

      // finally delete applications since
      // the application can not be deleted which has items.
      appDeleted <- applicationOp.delete(delApps)

    } yield {
      updated + deleted + appCreated + appDeleted
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
