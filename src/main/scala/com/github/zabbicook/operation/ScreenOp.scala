package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.screen.Screen
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScreenOp(api: ZabbixApi) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  def findByName(name: String): Future[Option[Screen[Stored]]] = {
    val params = Json.obj()
      .filter("name" -> name)
      .outExtend()
    api.requestSingleAs[Screen[Stored]]("screen.get", params)
  }

  def findByNameAbsolutely(name: String): Future[Screen[Stored]] = {
    findByName(name).map(_.getOrElse(throw NoSuchEntityException(s"No such screen: '$name'")))
  }

  def create(screen: Screen[NotStored]): Future[Report] = {
    val param = Json.toJson(screen).as[JsObject]
    api.requestSingleId("screen.create", param, "screenids")
      .map(id => Report.created(screen.toStored(id)))
  }

  def delete(screens: Seq[Screen[Stored]]): Future[Report] = {
    deleteEntities(api, screens, "screen.delete", "screenids")
  }

  def update(screenId: StoredId, screen: Screen[NotStored]): Future[Report] = {
    val param = Json.toJson(screen.toStored(screenId)).as[JsObject]
    api.requestSingleId("screen.update", param, "screenids")
      .map(id => Report.updated(screen.toStored(id)))
  }

  private[this] def present(config: Screen[NotStored]): Future[Report] = {
    findByName(config.name).flatMap {
      case Some(current) if current.shouldBeUpdated(config) =>
        update(current.getStoredId, config)
      case Some(_) =>
        Future.successful(Report.empty())
      case None =>
        create(config)
    }
  }

  def present(configs: Seq[Screen[NotStored]]): Future[Report] = {
    if (configs.isEmpty) {
      Future.successful(Report.empty())
    } else {
      for {
        _ <- showStartInfo(logger, configs.length, s"global screen")
        r <- traverseOperations(configs)(present)
      } yield r
    }
  }

  def absent(names: Seq[String]): Future[Report] = {
    for {
      founds <- Future.traverse(names)(findByName)
      r <- delete(founds.flatten)
    } yield r
  }
}
