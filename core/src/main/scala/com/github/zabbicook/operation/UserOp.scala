package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.{ErrorResponseException, ZabbixApi}
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.media.Media
import com.github.zabbicook.entity.user.{MediaConfig, User, UserConfig, UserGroup}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * User api
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/user
  */
class UserOp(api: ZabbixApi, userGroupOp: UserGroupOp, mediaTypeOp: MediaTypeOp) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

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
  def create(user: User[NotStored], groups: Seq[StoredId], password: String): Future[Report] = {
    val groupIds = groups.map(g => Json.obj("usrgrpid" -> g))
    val param = Json.toJson(user).as[JsObject]
      .prop("passwd" -> password)
      .prop("usrgrps" -> groupIds)
    api.requestSingleId("user.create", param, "userids")
      .map(id => Report.created(user.toStored(id)))
  }

  def update(id: StoredId, user: User[NotStored], groups: Seq[StoredId]): Future[Report] = {
    val param = user.toJsonForUpdate(id)
      .prop("usrgrps" -> groups.map(g => Json.obj("usrgrpid" -> g)))
    api.requestSingleId("user.update", param, "userids")
      .map(id => Report.updated(user.toStored(id)))
  }

  def updatePassword(user: User[Stored], password: String): Future[Report] = {
    val id = user.getStoredId
    val param = Json.obj(
      "userid" -> id,
      "passwd" -> password
    )
    api.requestSingleId("user.update", param, "userids")
      .map(_ => Report.updated(user))
  }

  def delete(users: Seq[User[Stored]]): Future[Report] = {
    deleteEntities(api, users, "user.delete", "userids")
  }

  def presentPassword(alias: String, presentedPass: String): Future[Report] = {
    findByAlias(alias) flatMap {
      case Some((user, _)) =>
        val params = Json.obj()
          .prop("user"->alias)
          .prop("password" -> presentedPass)
        // attempt to login
        api.request("user.login", params, auth = false)
          .map { _ =>
            logger.debug(s"presentPassword does nothing with: ${alias}")
            Report.empty()
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
  def present(userConf: UserConfig): Future[Report] = {
    for {
      (user, rUser) <- presentUser(userConf)
      rMedias <- userConf.media match {
        case Some(medias) => presentUserMedia(user, medias)
        case None => Future.successful(Report.empty())
      }
    } yield rUser + rMedias
  }

  private[this] def presentUser(userConf: UserConfig): Future[(User[Stored], Report)] = {
    val groupIdsFut = userGroupOp.findByNamesAbsolutely(userConf.groupNames)
      .map(_.map(_._1.getStoredId))

    val generate: Future[(User[Stored], Report)] = findByAlias(userConf.user.alias).flatMap {
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
          } yield (storedUser, results)

        } else {
          logger.debug(s"presentUser did nothing with: ${userConf.user.alias}")
          Future.successful((storedUser, Report.empty()))
        }
      case None =>
        logger.debug(s"presentUser attempt to create user: ${userConf.user.alias}")
        for {
          gids <- groupIdsFut
          results <- create(userConf.user, gids, userConf.password)
        } yield (results.created.head.asInstanceOf[User[Stored]], results)
    }

    for {
      (user, rGen) <- generate
      rPass <- if (userConf.initialPassword) {
        // If initialPassword is true, initial password has been set in generate()
        Future.successful(Report.empty())
      } else {
        presentPassword(userConf.user.alias, userConf.password)
      }
    } yield {
      (user, rGen + rPass)
    }
  }

  def findUserMedias(userId: StoredId): Future[Seq[Media[Stored]]] = {
    val params = Json.obj()
      .outExtend()
      .prop("userids" -> userId)
    api.requestAs[Seq[Media[Stored]]]("usermedia.get", params)
  }

  def mediaConfigToNotStoredMedia(mediaConfigs: Seq[MediaConfig], userAlias: String): Future[Seq[Media[NotStored]]] = {
    Future.traverse(mediaConfigs) { config =>
      mediaTypeOp.findByDescription(config.mediaType).map {
        case Some(mediaType) => config.toMedia(mediaType.getStoredId)
        case None => throw NoSuchEntityException(s"No such media type '${config.mediaType}' for user '$userAlias'.")
      }
    }
  }

  def addUserMedias(userId: StoredId, medias: Seq[Media[NotStored]]): Future[Report] = {
    val param = Json.obj()
      .prop("users" -> Json.obj().prop("userid" -> userId))
      .prop("medias" -> medias)
    api.requestIds("user.addmedia", param, "mediaids")
      .map{ids =>
        val entities = (ids zip medias) map { case (id, m) => m.toStored(id)}
        Report.created(entities)
      }
  }

  def deleteUserMedia(medias: Seq[Media[Stored]]): Future[Report] = {
    deleteEntities(api, medias, "user.deletemedia", "mediaids")
  }

  private[this] def presentUserMedia(user: User[Stored], medias: Seq[MediaConfig]): Future[Report] = {
    findUserMedias(user.getStoredId).flatMap { storedMedias =>
      mediaConfigToNotStoredMedia(medias, user.alias).flatMap { configs =>
        if (
          configs.length == storedMedias.length &&
          configs.foldLeft(storedMedias)((acc, config) => acc.find(_.isSame(config)).fold(acc)(acc diff List(_))).isEmpty &&
          storedMedias.foldLeft(configs)((acc, stored) => acc.find(_.isSame(stored)).fold(acc)(acc diff List(_))).isEmpty
        ) {
          // stored medias and configurations are exactly same
          Future.successful(Report.empty())
        } else {
          // differenet
          for {
            d <- deleteUserMedia(storedMedias)
            c <- addUserMedias(user.getStoredId, configs)
          } yield d + c
        }
      }
    }
  }

  /**
    * @param userAndPasswords triples of (user object, names of groups, password)
    */
  def present(userAndPasswords: Seq[UserConfig]): Future[Report] = {
    showStartInfo(logger, userAndPasswords.length, "users").flatMap(_ =>
      traverseOperations(userAndPasswords)(present)
    )
  }

  /**
    * @param aliases aliases of users to be deleted
    */
  def absent(aliases: Seq[String]): Future[Report] = {
    for {
      storedUsers <- findByAliases(aliases)
      r <- delete(storedUsers.map(_._1))
    } yield r
  }
}
