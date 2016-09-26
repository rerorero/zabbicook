package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.{ErrorResponseException, ZabbixApi}
import com.github.zabbicook.entity.User.UserId
import com.github.zabbicook.entity.UserGroup.UserGroupId
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

  def findByAlias(alias: String): Future[Option[(User, Seq[UserGroup])]] = {
    val params = Json.obj()
      .filter("alias" -> alias)
      .outExtend()
      .prop("selectUsrgrps" -> "extend")
    api.requestSingleAs[JsObject]("user.get", params).map(_.map(mapToUserGroupBandled))
  }

  private[this] def mapToUserGroupBandled(root: JsValue): (User, Seq[UserGroup]) = {
    val user = root.asOpt[User].getOrElse(sys.error(s"user.get returns unexpected formats.: ${Json.prettyPrint(root)}"))
    val userGroups = (root \ "usrgrps").asOpt[Seq[UserGroup]].getOrElse(sys.error(s"user.get returns no user groups"))
    (user, userGroups)
  }

  def findByAliases(aliases: Seq[String]): Future[Seq[(User, Seq[UserGroup])]] = {
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
  def create(user: User, groups: Seq[UserGroupId], password: String): Future[(UserId, Report)] = {
    val groupIds = groups.map(g => Json.obj("usrgrpid" -> g))
    val param = Json.toJson(user.removeReadOnly).as[JsObject]
      .prop("passwd" -> password)
      .prop("usrgrps" -> groupIds)
    api.requestSingleId[UserId]("user.create", param, "userids")
      .map((_, Report.created(user)))
  }

  def update(user: User, groups: Seq[UserGroupId]): Future[(UserId, Report)] = {
    val id = user.userid.getOrElse(sys.error(s"user does not have userid."))
    val param = Json.toJson(user.removeReadOnly.copy(userid = Some(id))).as[JsObject]
      .prop("usrgrps" -> groups.map(g => Json.obj("usrgrpid" -> g)))
    api.requestSingleId[UserId]("user.update", param, "userids")
      .map((_, Report.updated(user)))
  }

  def updatePassword(user: User, password: String): Future[(UserId, Report)] = {
    val id = user.userid.getOrElse(sys.error(s"User ${user.alias} to be deleted has no id."))
    val param = Json.obj(
      "userid" -> id,
      "passwd" -> password
    )
    api.requestSingleId[UserId]("user.update", param, "userids")
      .map((_, Report.updated(user)))
  }

  def delete(users: Seq[User]): Future[(Seq[UserId], Report)] = {
    if (users.isEmpty) {
      Future.successful((Seq(), Report.empty()))
    } else {
      val param = Json.toJson(users.map(_.userid.getOrElse(sys.error(s"user.delete requires user ids"))))
      api.requestIds[UserId]("user.delete", param, "userids")
        .map((_, Report.deleted(users)))
    }
  }

  def presentPassword(alias: String, presentedPass: String): Future[(UserId, Report)] = {
    findByAlias(alias) flatMap {
      case Some((user, _)) =>
        val params = Json.obj()
          .prop("user"->alias)
          .prop("password" -> presentedPass)
        // attempt to login
        api.request("user.login", params, auth = false)
          .map { _ =>
            val id = user.userid.getOrElse(sys.error("findByAlias returns no user id"))
            logger.debug(s"presentPassword does nothing with: ${alias}")
            (id, Report.empty())
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
  def present(userConf: UserConfig): Future[(UserId, Report)] = {
    val groupIdsFut = userGroupOp.findByNamesAbsolutely(userConf.groupNames.toSeq)
      .map(_.map(_._1.usrgrpid.getOrElse(sys.error(s"UserGroupOp returns no id."))))

    val generate = findByAlias(userConf.user.alias).flatMap {
      case Some((storedUser, storedGroups)) =>
        val id = storedUser.userid.getOrElse(sys.error("findByAlias returns no user id"))
        if (
          storedUser.shouldBeUpdated(userConf.user) ||
          storedGroups.map(_.name).toSet != userConf.groupNames.toSet
        ) {
          logger.debug(s"presentUser attempt to update user: ${userConf.user.alias}")
          for {
            gids <- groupIdsFut
            results <- update(userConf.user.copy(userid = Some(id)), gids)
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
  def present(userAndPasswords: Seq[UserConfig]): Future[(Seq[UserId], Report)] = {
    traverseOperations(userAndPasswords)(present)
  }

  /**
    * @param aliases aliases of users to be deleted
    */
  def absent(aliases: Seq[String]): Future[(Seq[UserId], Report)] = {
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
case class UserConfig(user: User, groupNames: Set[String], password: String)

object UserConfig {
  implicit val hoconReads: HoconReads[UserConfig] = {
    for {
      user <- of[User]
      groupNames <- required[Set[String]]("groups")
      password <- required[String]("password")
    } yield {
      UserConfig(user, groupNames, password)
    }
  }
}
