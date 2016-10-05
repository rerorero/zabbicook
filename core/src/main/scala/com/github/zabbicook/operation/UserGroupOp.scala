package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.{Permission, UserGroup, UserGroupPermission}
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/usergroup
  */
class UserGroupOp(api: ZabbixApi) extends OperationHelper with Logging {
  private[this] val hostGroupOp = new HostGroupOp(api)
  private[this] val logger = defaultLogger

  def findByName(name: String): Future[Option[(UserGroup[Stored], Seq[UserGroupPermission[Stored]])]] = {
    val param = Json.obj()
      .filter("name" -> name)
      .prop("selectRights" -> "extend")
      .outExtend()
    api.requestSingle("usergroup.get", param).map {
      _.map {
        case root: JsObject =>
          try {
            val userGroup = Json.fromJson[UserGroup[Stored]](root).get
            val rights = Json.fromJson[Seq[UserGroupPermission[Stored]]]((root \ "rights").get).get
            (userGroup, rights)
          } catch {
            case NonFatal(e) =>
              logger.error(s"usergroup.get returns: ${Json.prettyPrint(root)}", e)
              sys.error(s"usergroup.get($name) returns an invalid object.")
          }
        case els =>
          logger.error(s"usergroup.get returns: ${Json.prettyPrint(els)}")
          sys.error(s"usergroup.get($name) returns a non object.")
      }
    }
  }

  def findByNames(names: Seq[String]): Future[Seq[(UserGroup[Stored], Seq[UserGroupPermission[Stored]])]] = {
    Future.traverse(names)(name => findByName(name)).map(_.flatten)
  }

  /**
    * If any one of names does not exist, throw an Exception
    */
  def findByNamesAbsolutely(names: Seq[String]): Future[Seq[(UserGroup[Stored], Seq[UserGroupPermission[Stored]])]] = {
    findByNames(names).map { results =>
      if (results.length < names.length) {
        val notFounds = (names.toSet -- results.map(_._1.name).toSet).mkString(",")
        throw NoSuchEntityException(s"No such user groups are not found: ${notFounds}")
      }
      results
    }
  }

  /**
    * @param group user group
    * @param rights permissions to assign to the group
    * @return report of a created entity.
    *         throw an Exception if group already exists
    *
    */
  def create(
    group: UserGroup[NotStored],
    rights: Seq[UserGroupPermission[Stored]]
  ): Future[Report] = {
    val param = Json.toJson(group).as[JsObject]
        .prop("rights" -> rights)
    api.requestSingleId("usergroup.create", param, "usrgrpids")
      .map(id => Report.created(group.toStored(id)))
  }

  def update(
    group: UserGroup[Stored],
    rights: Seq[UserGroupPermission[Stored]]
  ): Future[Report] = {
    val param = Json.toJson(group).as[JsObject]
      .prop("rights" -> rights)
    api.requestSingleId("usergroup.update", param, "usrgrpids")
      .map(_ => Report.updated(group))
  }

  def delete(groups: Seq[UserGroup[Stored]]): Future[Report] = {
    deleteEntities(api, groups, "usergroup.delete", "usrgrpids")
  }

  /**
    * keep the status of the user group to be constant.
    * If the user group with the specified name does not exist, create it.
    * If already exists , it fills the gap.
    * @return User group id and operation state
    */
  def present(userGroup: UserGroupConfig): Future[Report] = {
    // convert to UserGroupPermission object
    val permissionsFut = hostGroupOp.findByNamesAbsolutely(userGroup.permissionsOfHosts.keys.toSeq).map {
      _.map { group =>
        UserGroupPermission[Stored](group.getStoredId, userGroup.permissionsOfHosts(group.name))
      }
    }

    for {
      permissions <- permissionsFut
      storedOpt <- findByName(userGroup.userGroup.name)
      result <- storedOpt match {
        case Some((storedUserGroup, storedPermissions)) =>
          val id = storedUserGroup.getStoredId
          if (
            storedUserGroup.shouldBeUpdated(userGroup.userGroup) ||
            storedPermissions.toSet != permissions.toSet
          ) {
            update(userGroup.userGroup.toStored(id), permissions)
          } else {
            Future.successful(Report.empty())
          }
        case None =>
          create(userGroup.userGroup, permissions)
      }
    } yield result
  }

  def present(groups: Seq[UserGroupConfig]): Future[Report] = {
    traverseOperations(groups)(present)
  }

  /**
    * @param groupNames names of gropus to be deleted
    */
  def absent(groupNames: Seq[String]): Future[Report] = {
    for {
      r <- findByNames(groupNames)
      report <- delete(r.map(_._1))
    } yield report
  }
}

/**
  * @param userGroup User group
  * @param permissionsOfHosts Pairs of name of a host group and a permission which describes the access level to the host.
  *                           The specified host groups must be presented before calling present() function.
  */
case class UserGroupConfig(userGroup: UserGroup[NotStored], permissionsOfHosts: Map[String, Permission])

object UserGroupConfig {
  implicit val hoconReads: HoconReads[UserGroupConfig] = {
    for {
      userGroup <- of[UserGroup[NotStored]]
      permission <- required[Map[String, Permission]]("permission")
    } yield {
      UserGroupConfig(userGroup, permission)
    }
  }
}
