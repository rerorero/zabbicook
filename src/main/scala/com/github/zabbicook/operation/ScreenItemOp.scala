package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityException
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.screen.ScreenItem._
import com.github.zabbicook.entity.screen._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScreenItemOp(
  api: ZabbixApi,
  templateOp: TemplateOp,
  screenOp: ScreenOp,
  templateScreenOp: TemplateScreenOp,
  graphOp: GraphOp,
  itemOp: ItemOp,
  hostGroupOp: HostGroupOp,
  hostOp: HostOp
) extends OperationHelper with Logging {

  def findByScreenId(
    parentTemplate: Option[String],
    screenId: StoredId
  ): Future[Seq[ScreenItem[Stored]]] = {
    val params = Json.obj()
      .prop("screenids" -> screenId)
      .outExtend()
    parentTemplate match {
      case Some(_) =>
        api.requestAs[Seq[ScreenItem[Stored]]]("templatescreenitem.get", params)
      case None =>
        api.requestAs[Seq[ScreenItem[Stored]]]("screenitem.get", params)
    }
  }

  def create(
    screenId: StoredId,
    item: ScreenItem[NotStored]
  ): Future[Report] = {
    val param = Json.toJson(item.withScreenId(screenId)).as[JsObject]
    api.requestSingleId("screenitem.create", param, "screenitemids")
      .map(id => Report.created(item.toStored(id)))
  }

  def resolveResource(screenId: StoredId, parentTemplate: Option[String], item: ScreenItemSetting): Future[ScreenItem[NotStored]] = {
    // Consistency between item.resourcetype and item.resource is checked at ScreenSetting.validate()
    val templateOpt = item.resource.flatMap(_.template).orElse(parentTemplate)
    val nameOpt = item.resource.map(_.name)

    item.resourcetype match {
      case ScreenResourceType.URL | ScreenResourceType.historyOfActions | ScreenResourceType.historyOfEvents |
            ScreenResourceType.statusOfZabbix | ScreenResourceType.clock | ScreenResourceType.hostsInfo |
            ScreenResourceType.systemStatus | ScreenResourceType.unknown =>
        Future.successful(item.toScreenItem(screenId, None))

      case ScreenResourceType.graph =>
        (templateOpt, nameOpt) match {
          case (Some(template), Some(name)) =>
            for {
              t <- templateOp.findByHostnameAbsolutely(template)
              graph <- graphOp.findByNameAbsolutelyFromAll(t.template.getStoredId, name)
            } yield item.toScreenItem(screenId, Some(graph.getStoredId))
          case (_, _) =>
            Future.failed(EntityException(s"A template graph name is required in 'resource' field of screen item of ${item.resourcetype}."))
        }

      case ScreenResourceType.simpleGraph | ScreenResourceType.plainText =>
        (templateOpt, nameOpt) match {
          case (Some(template), Some(name)) =>
            for {
              t <- templateOp.findByHostnameAbsolutely(template)
              templateItem <- itemOp.findByNameAndHostAbsolutely(name, t.template.getStoredId)
            } yield item.toScreenItem(screenId, Some(templateItem.getStoredId))
          case (_, _) =>
            Future.failed(EntityException(s"A template item name is required in 'resource' field of screen item of ${item.resourcetype}."))
        }

      case ScreenResourceType.triggersInfo | ScreenResourceType.triggersOverview |
           ScreenResourceType.dataOverview | ScreenResourceType.hostGroupIssues =>
        nameOpt match {
          case Some(name) =>
            hostGroupOp.findByNameAbsolutely(name).map(hostGruop => item.toScreenItem(screenId, Some(hostGruop.getStoredId)))
          case None =>
            Future.failed(EntityException(s"A host group name is required in 'resource' field of screen item of ${item.resourcetype}."))
        }

      case ScreenResourceType.screen =>
        (templateOpt, nameOpt) match {
          case (Some(template), Some(name)) =>
            for {
              t <- templateOp.findByHostnameAbsolutely(template)
              templateScreen <- templateScreenOp.findByNameAbsolutely(t.template.getStoredId, name)
            } yield item.toScreenItem(screenId, Some(templateScreen.getStoredId))
          case (None, Some(name)) =>
            screenOp.findByNameAbsolutely(name).map(screen => item.toScreenItem(screenId, Some(screen.getStoredId)))
          case (_, None) =>
            Future.failed(EntityException(s"A screen name is required in 'resource' field of screen item of ${item.resourcetype}."))
        }

      case hostIssues =>
        nameOpt match {
          case Some(name) =>
            hostOp.findByHostnameAbsolutely(name).map(host => item.toScreenItem(screenId, Some(host.host.getStoredId)))
          case None =>
            Future.failed(EntityException(s"A host name is required in 'resource' field of screen item of ${item.resourcetype}."))
        }
    }
  }

  def delete(items: Seq[ScreenItem[Stored]]): Future[Report] = {
    deleteEntities(api, items, "screenitem.delete", "screenitemids")
  }

  private[this] def shouldBeUpdated(configs: Seq[ScreenItem[NotStored]], current: Seq[ScreenItem[Stored]]): Boolean = {
    val noNeedToUpdate = current.length == configs.length &&
      (current.sortBy(_.key) zip configs.sortBy(_.key)).forall {
        case ((stored, config)) => !stored.shouldBeUpdated(config)
      }
    !noNeedToUpdate
  }

  private[this] def findScreenByName(
    parentTemplate: Option[String],
    name: String
  ): Future[Screen[Stored]] = {
    parentTemplate match {
      case Some(parent) =>
        for {
          t <- templateOp.findByHostnameAbsolutely(parent)
          screen <- templateScreenOp.findByNameAbsolutely(t.template.getStoredId, name)
        } yield screen
      case None =>
        screenOp.findByNameAbsolutely(name)
    }
  }

  private[this] def recreatesAll(
    screenId: StoredId,
    currentItems: Seq[ScreenItem[Stored]],
    resolvedConfig: Seq[ScreenItem[NotStored]]
  ): Future[Report] = {
    for {
      deletes <- delete(currentItems)
      creates <- traverseOperations(resolvedConfig)(create(screenId, _))
    } yield deletes + creates
  }

  def present(screenName: String, configs: Seq[ScreenItemSetting], parentTemplate: Option[String]): Future[Report] = {
    for {
      screen <- findScreenByName(parentTemplate, screenName)
      currentItems <- findByScreenId(parentTemplate, screen.getStoredId)
      resolvedConfig <- Future.traverse(configs)(resolveResource(screen.getStoredId, parentTemplate, _))
      update = shouldBeUpdated(resolvedConfig, currentItems)
      // Rebuild items if they should be updated.
      reports <- if (update) {
        recreatesAll(screen.getStoredId, currentItems, resolvedConfig)
      } else {
        Future.successful(Report.empty())
      }
    } yield reports
  }
}
