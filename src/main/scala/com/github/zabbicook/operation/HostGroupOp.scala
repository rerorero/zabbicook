package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.host.HostGroup
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HostGroupOp(api: ZabbixApi) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  def findByName(name: String): Future[Option[HostGroup[Stored]]] = {
    api.requestSingleAs[HostGroup[Stored]]("hostgroup.get", Json.obj().filter("name" -> name).outExtend())
  }

  def findByNames(names: Seq[String]): Future[Seq[HostGroup[Stored]]] = {
    Future.traverse(names)(findByName).map(_.flatten)
  }

  /**
    * If any one of names does not exist, throw an Exception
    */
  def findByNamesAbsolutely(names: Seq[String]): Future[Seq[HostGroup[Stored]]] = {
    findByNames(names).map { results =>
      if (results.length < names.length) {
        val notFounds = (names.toSet -- results.map(_.name).toSet).mkString(",")
        throw NoSuchEntityException(s"No such host groups: ${notFounds}")
      }
      results
    }
  }

  def findByNameAbsolutely(name: String): Future[HostGroup[Stored]] = {
    findByName(name).map(_.getOrElse(throw NoSuchEntityException(s"No such host group: ${name}")))
  }

  def create(group: HostGroup[NotStored]): Future[Report] = {
    val param = Json.toJson(group)
    api.requestSingleId("hostgroup.create", param, "groupids")
      .map(id => Report.created(group.toStored(id)))
  }

  /**
    * A host group can not be deleted if:
    * - it contains hosts that belong to this group only;
    * - it is marked as internal;
    * - it is used by a host prototype;
    * - it is used in a global script;
    * - it is used in a correlation condition.
    */
  def delete(groups: Seq[HostGroup[Stored]]): Future[Report] = {
    deleteEntities(api, groups, "hostgroup.delete", "groupids")
  }

  // There are no writable properties of HostGroup,
  // so we do not have update() method.

  /**
    * Keep the status of the HostGroup to be constant.
    * If the host group specified name does not exist, create it.
    * If already exists, it fills the gap.
    */
  def present(group: HostGroup[NotStored]): Future[Report] = {
    findByName(group.name).flatMap {
      case Some(stored) =>
        Future.successful(Report.empty())
      case None =>
        create(group)
    }
  }

  def present(groups: Seq[HostGroup[NotStored]]): Future[Report] = {
    showStartInfo(logger, groups.length, s"host groups").flatMap(_ =>
      traverseOperations(groups)(present)
    )
  }

  /**
    * @param names names of host groups to be deleted
    * @return
    */
  def absent(names: Seq[String]): Future[Report] = {
    findByNames(names).flatMap(delete)
  }
}

