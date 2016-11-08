package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.media.MediaType
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MediaTypeOp(api: ZabbixApi) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  def findByDescription(name: String): Future[Option[MediaType[Stored]]] = {
    val params = Json.obj()
      .filter("description" -> Seq(name))
      .outExtend()
    api.requestSingleAs[MediaType[Stored]]("mediatype.get", params)
  }

  def findByDescriptions(names: Seq[String]): Future[Seq[MediaType[Stored]]] = {
    Future.traverse(names)(findByDescription).map(_.flatten)
  }

  def create(mediatype: MediaType[NotStored]): Future[Report] = {
    val param = Json.toJson(mediatype).as[JsObject]
    api.requestSingleId("mediatype.create", param, "mediatypeids")
      .map(id => Report.created(mediatype.toStored(id)))
  }

  def delete(mediaTypes: Seq[MediaType[Stored]]): Future[Report] = {
    deleteEntities(api, mediaTypes, "mediatype.delete", "mediatypeids")
  }

  def update(current: MediaType[Stored], modified: MediaType[NotStored]): Future[Report] = {
    val param = modified.toJsonForUpdate(current.getStoredId)
    api.requestSingleId("mediatype.update", param, "mediatypeids")
      .map(id => Report.updated(modified.toStored(id)))
  }

  def present(mediaType: MediaType[NotStored]): Future[Report] = {
    findByDescription(mediaType.description).flatMap {
      case Some(stored) if stored.shouldBeUpdated(mediaType)=>
        update(stored, mediaType)
      case Some(stored) =>
        Future.successful(Report.empty())
      case None =>
        create(mediaType)
    }
  }

  def present(mediaTypes: Seq[MediaType[NotStored]]): Future[Report] = {
    showStartInfo(logger, mediaTypes.length, "mediatype").flatMap(_ =>
      traverseOperations(mediaTypes)(present)
    )
  }

  def absent(descriptions: Seq[String]): Future[Report] = {
    findByDescriptions(descriptions).flatMap(delete)
  }
}
