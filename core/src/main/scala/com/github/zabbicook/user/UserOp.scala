package com.github.zabbicook.user

import com.github.zabbicook.api.{ErrorResponseException, ZabbixApi}
import com.github.zabbicook.operation.{NoSuchEntityException, OperationHelper, Report}
import com.github.zabbicook.user.User.UserId
import com.github.zabbicook.user.UserGroup.UserGroupId
import com.github.zabbicook.{LoggerName, Logging}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * User api
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/user
  */
class UserOp(api: ZabbixApi) extends OperationHelper with Logging {

  private[this] val logger = loggerOf(LoggerName.Api)

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
          .prop("passwd" -> presentedPass)
        // attempt to login
        api.request("user.login", params, auth = false)
          .map { _ =>
            val id = user.userid.getOrElse(sys.error("findByAlias returns no user id"))
            logger.debug(s"presentPassword does nothing with: ${alias}")
            (id, Report.empty())
          }
          .recoverWith {
          case ErrorResponseException(_, response, _) if response.data.contains("incorrect") =>
            logger.debug(s"presentPassword does nothing with: ${alias}")
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
  def present(user: User, userGroupNames: Seq[String], password: String): Future[(UserId, Report)] = {
    val groupIdsFut = userGroupOp.findByNamesAbsolutely(userGroupNames)
      .map(_.map(_._1.usrgrpid.getOrElse(sys.error(s"UserGroupOp returns no id."))))

    findByAlias(user.alias).flatMap {
      case Some((storedUser, storedGroups)) =>
        val id = storedUser.userid.getOrElse(sys.error("findByAlias returns no user id"))
        if (
          storedUser.shouldBeUpdated(user) ||
          storedGroups.map(_.name).toSet != userGroupNames.toSet
        ) {
          logger.debug(s"presentUser attempt to update user: ${user.alias}")
          for {
            gids <- groupIdsFut
            results <- update(user.copy(userid = Some(id)), gids)
          } yield results

        } else {
          logger.debug(s"presentUser did nothing with: ${user.alias}")
          Future.successful((id, Report.empty()))
        }
      case None =>
        logger.debug(s"presentUser attempt to create user: ${user.alias}")
        for {
          gids <- groupIdsFut
          results <- create(user, gids, password)
        } yield results
    }
  }

  /**
    * @param userAndPasswords triples of (user object, names of groups, password)
    */
  def present(userAndPasswords: Seq[(User, Seq[String], String)]): Future[(Seq[UserId], Report)] = {
    traverseOperations(userAndPasswords)(uap => present(uap._1, uap._2, uap._3))
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
