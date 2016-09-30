package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.{ErrorResponseException, ZabbixApi}
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.{User, UserGroup}
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * User api
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/user
  */
class UserOp(api: ZabbixApi) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  private[this] lazy val userGroupOp = new UserGroupOp(api)

  def findByAlias(alias: String): Future[Option[(User[Stored], Seq[UserGroup[Stored]])]] = {
    val params = Json.obj()
      .filter("alias" -> alias)
      .outExtend()
      .prop("selectUsrgrps" -> "extend")
    api.requestSingleAs[JsObject]("user.get", params).map(_.map(mapToUserGroupBandled))
  }

  private[this] def mapToUserGroupBandled(root: JsValue): (User[Stored], Seq[UserGroup[Stored]]) = {
    val user = root.asOpt[User[Stored]].getOrElse(sys.error(s"user.get returns unexpected formats.: ${Json.prettyPrint(root)}"))
    val userGroups = (root \ "usrgrps").asOpt[Seq[UserGroup[Stored]]].getOrElse(sys.error(s"user.get returns no user groups"))
    (user, userGroups)
  }

  def findByAliases(aliases: Seq[String]): Future[Seq[(User[Stored], Seq[UserGroup[Stored]])]] = {
    if (aliases.isEmpty) {
      Future.successful(Seq())
    } else {
      val params = Json.obj()
        .filter("alias" -> aliases)
        .outExtend()
        .prop("selectUsrgrps" -> "extend")
      api.requestAs[Seq[JsObject]]("user.get", params).map(_.map(mapToUserGroupBandled))
    }
  }

  /**
    * throw an Exception if already exists
    */
  def create(user: User[NotStored], groups: Seq[StoredId], password: String): Future[(StoredId, Report)] = {
    val groupIds = groups.map(g => Json.obj("usrgrpid" -> g))
    val param = Json.toJson(user).as[JsObject]
      .prop("passwd" -> password)
      .prop("usrgrps" -> groupIds)
    api.requestSingleId("user.create", param, "userids")
      .map(id => (id, Report.created(user.toStored(id))))
  }

  def update(id: StoredId, user: User[NotStored], groups: Seq[StoredId]): Future[(StoredId, Report)] = {
    val param = user.toJsonForUpdate(id)
      .prop("usrgrps" -> groups.map(g => Json.obj("usrgrpid" -> g)))
    api.requestSingleId("user.update", param, "userids")
      .map(id => (id, Report.updated(user.toStored(id))))
  }

  def updatePassword(user: User[Stored], password: String): Future[(StoredId, Report)] = {
    val id = user.getStoredId
    val param = Json.obj(
      "userid" -> id,
      "passwd" -> password
    )
    api.requestSingleId("user.update", param, "userids")
      .map((_, Report.updated(user)))
  }

  def delete(users: Seq[User[Stored]]): Future[(Seq[StoredId], Report)] = {
    if (users.isEmpty) {
      Future.successful((Seq(), Report.empty()))
    } else {
      val param = Json.toJson(users.map(_.getStoredId))
      api.requestIds("user.delete", param, "userids")
        .map((_, Report.deleted(users)))
    }
  }

  def presentPassword(alias: String, presentedPass: String): Future[(StoredId, Report)] = {
    findByAlias(alias) flatMap {
      case Some((user, _)) =>
        val params = Json.obj()
          .prop("user"->alias)
          .prop("password" -> presentedPass)
        // attempt to login
        api.request("user.login", params, auth = false)
          .map { _ =>
            logger.debug(s"presentPassword does nothing with: ${alias}")
            (user.getStoredId, Report.empty())
          }
          .recoverWith {
          case ErrorResponseException(_, response, _) if response.data.contains("incorrect") =>
            logger.debug(s"presentPassword will change password for: ${alias}")
            updatePassword(user, presentedPass)
        }
      case None =>
        Future.failed(NoSuchEntityException(s"No such user: ${alias} "))
    }
  }

  /**
    * Keep the status of the user to be constant.
    * If the user with the specified alias does not exist, create it.
    * If already exists, it fills the gap.
    */
  def present(userConf: UserConfig): Future[(StoredId, Report)] = {
    val groupIdsFut = userGroupOp.findByNamesAbsolutely(userConf.groupNames.toSeq)
      .map(_.map(_._1.getStoredId))

    val generate = findByAlias(userConf.user.alias).flatMap {
      case Some((storedUser, storedGroups)) =>
        val id = storedUser.getStoredId
        if (
          storedUser.shouldBeUpdated(userConf.user) ||
          storedGroups.map(_.name).toSet != userConf.groupNames.toSet
        ) {
          logger.debug(s"presentUser attempt to update user: ${userConf.user.alias}")
          for {
            gids <- groupIdsFut
            results <- update(id, userConf.user, gids)
          } yield results

        } else {
          logger.debug(s"presentUser did nothing with: ${userConf.user.alias}")
          Future.successful((id, Report.empty()))
        }
      case None =>
        logger.debug(s"presentUser attempt to create user: ${userConf.user.alias}")
        for {
          gids <- groupIdsFut
          results <- create(userConf.user, gids, userConf.password)
        } yield results
    }

    for {
      (uid, rGen) <- generate
      (_, rPass) <- presentPassword(userConf.user.alias, userConf.password)
    } yield {
      (uid, rGen + rPass)
    }
  }

  /**
    * @param userAndPasswords triples of (user object, names of groups, password)
    */
  def present(userAndPasswords: Seq[UserConfig]): Future[(Seq[StoredId], Report)] = {
    traverseOperations(userAndPasswords)(present)
  }

  /**
    * @param aliases aliases of users to be deleted
    */
  def absent(aliases: Seq[String]): Future[(Seq[StoredId], Report)] = {
    for {
      storedUsers <- findByAliases(aliases)
      tpl <- delete(storedUsers.map(_._1))
    } yield tpl
  }
}

/**
  * @param user user object
  * @param groupNames names of groups to which the user belongs
  * @param password password (it is used only if the user does not exist yet)
  * TODO: Do we need to separate the password from here?
  */
case class UserConfig(user: User[NotStored], groupNames: Set[String], password: String)

object UserConfig {
  implicit val hoconReads: HoconReads[UserConfig] = {
    for {
      user <- of[User[NotStored]]
      groupNames <- required[Set[String]]("groups")
      password <- required[String]("password")
    } yield {
      UserConfig(user, groupNames, password)
    }
  }
}
