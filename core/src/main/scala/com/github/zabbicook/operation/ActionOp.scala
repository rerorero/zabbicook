package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.action._
import com.github.zabbicook.entity.{EntityState, PropCompare}
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * entities of action + action filter + operations + recovery operations
  */
case class ActionEntitySet[S <: EntityState](
  action: Action[S],
  filter: ActionFilter[S],
  operations: Seq[ActionOperation[S]],
  recoveryOperations: Option[Seq[RecoveryActionOperation[S]]]
) extends PropCompare with OperationHelper {

  def shouldBeUpdated[T >: S <: Stored](constant: ActionEntitySet[NotStored]): Boolean = {
    val operationsShouldBeUpdated = !(
      (operations.length == constant.operations.length) &&
      constant.operations.forall(cc => operations.exists(_.isSame[T](cc))))

    val recoveriesShouldBeUpdated = (recoveryOperations, constant.recoveryOperations) match {
      case (Some(s), Some(c)) =>
        val isSame = (s.length == c.length) && c.forall(cc => s.exists(_.isSame[T](cc)))
        !isSame
      case (None, None) | (Some(Seq()), None) | (None, Some(Seq())) => false
      case _ => true
    }

    action.shouldBeUpdated[T](constant.action) ||
    filter.shouldBeUpdated[T](constant.filter) ||
    operationsShouldBeUpdated ||
    recoveriesShouldBeUpdated
  }
}

object ActionEntitySet extends OperationHelper {
  implicit val reads: Reads[ActionEntitySet[Stored]] = Reads[ActionEntitySet[Stored]] { root: JsValue =>
    val action = root.asOpt[Action[Stored]].getOrElse(sys.error(s"action.get returns unexpected formats.: ${Json.prettyPrint(root)}"))
    val filter = (root \ "filter").asOpt[ActionFilter[Stored]].getOrElse(ActionFilter.empty)
    val operations = (root \ "operations").asOpt[Seq[ActionOperation[Stored]]].getOrElse(sys.error(s"action.get returns no operations"))
    val recoveryActionOperation = (root \ "recoveryOperations").asOpt[Seq[RecoveryActionOperation[Stored]]]
    JsSuccess(ActionEntitySet(action, filter, operations, recoveryActionOperation))
  }

  implicit val writes: Writes[ActionEntitySet[NotStored]] = Writes[ActionEntitySet[NotStored]] { entitySet =>
    require(entitySet.operations.forall(_.isMediaTypeIdSet))
    require(entitySet.recoveryOperations.map(_.forall(_.isMediaTypeIdSet)).getOrElse(true))
    Json.toJson(entitySet.action).as[JsObject]
      .prop("filter" -> entitySet.filter)
      .prop("operations" -> entitySet.operations)
      .propOpt("recovery_operations" -> entitySet.recoveryOperations)
  }

  private val transformForUpdate: Reads[JsObject] = (__ \ "operations").json.update(
      __.read[JsArray].map(ary => JsArray(ary.value.map(_.transform(ActionOperation.transformerForUpdate).get)))
    )

  private val transformForUpdateWithRecover: Reads[JsObject] = transformForUpdate.andThen(
    (__ \ "recovery_operations").json.update(
      __.read[JsArray].map(ary => JsArray(ary.value.map(_.transform(RecoveryActionOperation.transformerForUpdate).get))
      )
    )
  )

  def toJsonForUpdate(entity: ActionEntitySet[NotStored], actionId: StoredId): Try[JsObject] = Try {
    val obj = Json.toJson(entity).as[JsObject]
      .prop("actionid" -> actionId) - "eventsource" // eventsource is constant.
    if (entity.recoveryOperations.isEmpty)
      obj.transform(transformForUpdate).get
    else
      obj.transform(transformForUpdateWithRecover).get
  }
}

class ActionOp(api: ZabbixApi, userGroupOp: UserGroupOp, userOp: UserOp, mediaTypeOp: MediaTypeOp) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  def findByName(name: String): Future[Option[ActionEntitySet[Stored]]] = {
    val params = Json.obj()
      .filter("name" -> name)
      .outExtend()
      .prop("selectFilter" -> "extend")
      .prop("selectOperations" -> "extend")
      .prop("selectRecoveryOperations" -> "extend")
    api.requestSingleAs[ActionEntitySet[Stored]]("action.get", params)
  }

  def findByNames(names: Seq[String]): Future[Seq[ActionEntitySet[Stored]]] = {
    Future.traverse(names)(findByName).map(_.flatten)
  }

  def findActionsByNames(names: Seq[String]): Future[Seq[Action[Stored]]] = {
    if (names.isEmpty) {
      Future.successful(Seq())
    } else {
      val params = Json.obj()
        .filter("name" -> names)
        .outExtend()
      api.requestAs[Seq[Action[Stored]]]("action.get", params)
    }
  }

  def create(entity: ActionEntitySet[NotStored]): Future[Report] = {
    val param = Json.toJson(entity)
    api.requestSingleId("action.create", param, "actionids")
      .map(id => Report.created(entity.action.toStored(id)))
  }

  def delete(actions: Seq[Action[Stored]]): Future[Report] = {
    deleteEntities(api, actions, "action.delete", "actionids")
  }

  def update(entity: ActionEntitySet[NotStored], actionId: StoredId): Future[Report] = {
    Future.fromTry(ActionEntitySet.toJsonForUpdate(entity, actionId)).flatMap(param =>
    api.requestSingleId("action.update", param, "actionids")
      .map(id => Report.updated(entity.action.toStored(id)))
    )
  }

  /**
    * to resolve media type id, user group id, and user id.
    */
  def configToNotStored(config: ActionConfig): Future[ActionEntitySet[NotStored]] = {
    def parse(
      userGroupNames: Option[Seq[String]],
      userNames: Option[Seq[String]],
      message: Option[OpMessageConfig]
    ): Future[(Option[Seq[OpMessageGroup]], Option[Seq[OpMessageUser]], Option[StoredId])] = {
      for {
        messageUserGroups <- userGroupNames.futureMap { groupNames =>
          userGroupOp.findByNames(groupNames).map(_.map(_._1.getStoredId).map(OpMessageGroup.apply))
        }
        messageUsers <- userNames.futureMap { userNames =>
          userOp.findByAliases(userNames).map(_.map(_._1.getStoredId).map(OpMessageUser.apply))
        }
        mediaType <- message.flatFutureMap { message =>
          message.mediaType.futureMap { name =>
            mediaTypeOp.findByDescription(name).map(
              _.getOrElse(throw NoSuchEntityException(s"No such media type is not found : ${name} (in action '${config.name}')"))
            )
          }
        }
      } yield {
        (messageUserGroups, messageUsers, mediaType.map(_.getStoredId))
      }
    }

    for {
      operations <-
        Future.traverse(config.operations) { opConf =>
          parse(opConf.opmessage_grp, opConf.opmessage_usr, opConf.message).map(t => opConf.toNotStored(t._1, t._2, t._3))
        }
      recoveries <- config.recoveryOperations.futureMap { opConfs =>
        Future.traverse(opConfs) { opConf =>
          parse(opConf.opmessage_grp, opConf.opmessage_usr, opConf.message).map(t => opConf.toNotStored(t._1, t._2, t._3))
        }
      }
    } yield {
      ActionEntitySet[NotStored](
        action = config.toNotStoredAction,
        filter = config.filter,
        operations = operations,
        recoveryOperations = recoveries
      )
    }
  }

  def present(actionConfig: ActionConfig): Future[Report] = {
    for {
      constant <- configToNotStored(actionConfig)
      storedOpt <- findByName(actionConfig.name)
      report <- storedOpt match {
        case None =>
          configToNotStored(actionConfig).flatMap(create)
        case Some(stored) if stored.shouldBeUpdated(constant) =>
          update(constant, stored.action.getStoredId)
        case Some(stored) =>
          Future.successful(Report.empty())
      }
    } yield report
  }

  def present(actionConfig: Seq[ActionConfig]): Future[Report] = {
    showStartInfo(logger, actionConfig.length, "actions").flatMap(_ =>
      traverseOperations(actionConfig)(present)
    )
  }

  def absent(names: Seq[String]): Future[Report] = {
    for {
      stored <- findActionsByNames(names)
      r <- delete(stored)
    } yield r
  }
}
