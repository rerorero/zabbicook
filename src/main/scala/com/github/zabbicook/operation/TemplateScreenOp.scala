package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.screen.Screen
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TemplateScreenOp(api: ZabbixApi, templateOp: TemplateOp) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  def findByName(templateId: StoredId, name: String): Future[Option[Screen[Stored]]] = {
    val params = Json.obj()
      .prop("templateids" -> templateId)
      .filter("name" -> name)
      .outExtend()
    api.requestSingleAs[Screen[Stored]]("templatescreen.get", params)
  }

  def findByNameAbsolutely(templateId: StoredId, name: String): Future[Screen[Stored]] = {
    findByName(templateId, name).map(_.getOrElse(throw NoSuchEntityException(s"No such template screen: '$name'")))
  }

  def create(templateId: StoredId, screen: Screen[NotStored]): Future[Report] = {
    val param = Json.toJson(screen).as[JsObject] + ("templateid" -> Json.toJson(templateId))
    api.requestSingleId("templatescreen.create", param, "screenids")
      .map(id => Report.created(screen.toStored(id)))
  }

  def delete(screens: Seq[Screen[Stored]]): Future[Report] = {
    deleteEntities(api, screens, "templatescreen.delete", "screenids")
  }

  def update(screenId: StoredId, screen: Screen[NotStored]): Future[Report] = {
    val param = Json.toJson(screen.toStored(screenId)).as[JsObject]
    api.requestSingleId("templatescreen.update", param, "screenids")
      .map(id => Report.updated(screen.toStored(id)))
  }

  private[this] def present(templateId: StoredId, config: Screen[NotStored]): Future[Report] = {
    findByName(templateId, config.name).flatMap {
      case Some(current) if current.shouldBeUpdated(config) =>
        update(current.getStoredId, config)
      case Some(_) =>
        Future.successful(Report.empty())
      case None =>
        create(templateId, config)
    }
  }

  def present(templateName: String, configs: Seq[Screen[NotStored]]): Future[Report] = {
    for {
      _ <- showStartInfo(logger, configs.length, s"screen of template '$templateName'")
      template <- templateOp.findByHostnameAbsolutely(templateName)
      r <- traverseOperations(configs)(present(template.template.getStoredId, _))
    } yield r
  }

  def absent(templateName: String, names: Seq[String]): Future[Report] = {
    templateOp.findByHostname(templateName).flatMap {
      case Some(template) =>
        for {
          founds <- Future.traverse(names)(findByName(template.template.getStoredId, _))
          r <- delete(founds.flatten)
        } yield r
      case None =>
        Future.successful(Report.empty())
    }
  }
}
