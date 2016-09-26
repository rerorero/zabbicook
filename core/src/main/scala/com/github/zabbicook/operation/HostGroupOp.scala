package com.github.zabbicook.operation

import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.HostGroup
import com.github.zabbicook.entity.HostGroup.HostGroupId
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class HostGroupOp(api: ZabbixApi) extends OperationHelper {

  def findByName(name: String): Future[Option[HostGroup]] = {
    api.requestSingleAs[HostGroup]("hostgroup.get", Json.obj().filter("name" -> name).outExtend())
  }

  def findByNames(names: Seq[String]): Future[Seq[HostGroup]] = {
    Future.traverse(names)(findByName).map(_.flatten)
  }

  /**
    * If any one of names does not exist, throw an Exception
    */
  def findByNamesAbsolutely(names: Seq[String]): Future[Seq[HostGroup]] = {
    findByNames(names).map { results =>
      if (results.length < names.length) {
        val notFounds = (names.toSet -- results.map(_.name).toSet).mkString(",")
        throw NoSuchEntityException(s"No such host groups are not found: ${notFounds}")
      }
      results
    }
  }

  def create(group: HostGroup): Future[(HostGroupId, Report)] = {
    val param = Json.toJson(group.removeReadOnly)
    api.requestSingleId[HostGroupId]("hostgroup.create", param, "groupids")
      .map((_, Report.created(group)))
  }

  /**
    * A host group can not be deleted if:
    * - it contains hosts that belong to this group only;
    * - it is marked as internal;
    * - it is used by a host prototype;
    * - it is used in a global script;
    * - it is used in a correlation condition.
    */
  def delete(groups: Seq[HostGroup]): Future[(Seq[HostGroupId], Report)] = {
    if (groups.isEmpty) {
      Future.successful((Seq(), Report.empty()))
    } else {
      val ids = groups.map(g => g.groupid.getOrElse(sys.error(s"HostGroup ${g.name} to be deleted has no id.")))
      val param = Json.toJson(ids)
      api.requestIds[HostGroupId]("hostgroup.delete", param, "groupids")
        .map((_, Report.deleted(groups)))
    }
  }

  // There are no writable properties of HostGroup,
  // so we do not have update() method.

  def present(group: HostGroup): Future[(HostGroupId, Report)] = {
    findByName(group.name).flatMap {
      case Some(stored) =>
        val id = stored.groupid.getOrElse(sys.error(s"HostGroup.findByName returns no id."))
        Future.successful((id, Report.empty()))
      case None =>
        create(group)
    }
  }

  def present(groups: Seq[HostGroup]): Future[(Seq[HostGroupId], Report)] = {
    traverseOperations(groups)(present)
  }

  /**
    * @param names names of host groups to be deleted
    * @return
    */
  def absent(names: Seq[String]): Future[(Seq[HostGroupId], Report)] = {
    findByNames(names).flatMap(delete)
  }
}

