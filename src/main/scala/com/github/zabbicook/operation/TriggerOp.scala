package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.template.Template
import com.github.zabbicook.entity.trigger.{Trigger, TriggerConf, TriggerTag}
import com.github.zabbicook.entity.{EntityState, PropCompare}
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TriggerSet[S <: EntityState] (
  trigger: Trigger[S],
  dependencies: Option[Seq[StoredId]],
  tags: Option[Seq[TriggerTag]]
) extends PropCompare {
  def shouldBeUpdated[T >: S <: Stored](constant: TriggerSet[NotStored]): Boolean = {
    trigger.shouldBeUpdated[T](constant.trigger) ||
      !isSameProp(dependencies.map(_.toSet), constant.dependencies.map(_.toSet), Set()) ||
      !isSameProp(tags.map(_.toSet), constant.tags.map(_.toSet), Set())
  }
  def toStored(triggerId: StoredId): TriggerSet[Stored] = copy(trigger = trigger.toStored(triggerId))
}

object TriggerSet extends OperationHelper {

  case class TriggerId(triggerid: StoredId)
  implicit val triggerIdFormat: Format[TriggerId] = Json.format[TriggerId]

  implicit val reads: Reads[TriggerSet[Stored]] = Reads[TriggerSet[Stored]] { root: JsValue =>
    val entity = root.asOpt[Trigger[Stored]].getOrElse(sys.error(s"trigger.get returns unexpected formats.: ${Json.prettyPrint(root)}"))
    val tags = (root \ "tags").asOpt[Seq[TriggerTag]]
    val dependencies = (root \ "dependencies").asOpt[Seq[TriggerId]].map(_.map(_.triggerid))
    JsSuccess(TriggerSet[Stored](entity, dependencies, tags))
  }

  private[this] def _writes[T <: EntityState](implicit triggerWrites: Writes[Trigger[T]]): Writes[TriggerSet[T]] = Writes[TriggerSet[T]] { entitySet =>
    val dependencies = entitySet.dependencies.map(_.map(TriggerId))
    Json.toJson[Trigger[T]](entitySet.trigger).as[JsObject]
      .propOpt("tags" -> entitySet.tags)
      .propOpt("dependencies" -> dependencies)
  }
  implicit val writes: Writes[TriggerSet[NotStored]] = _writes[NotStored]
  implicit val writes2: Writes[TriggerSet[Stored]] = _writes[Stored]
}

class TriggerOp(api: ZabbixApi, templateOp: TemplateOp) extends OperationHelper with Logging {
  private[this] val logger = defaultLogger

  /**
    * get triggers inherited from parent templates
    */
  def getInheritedTriggers(hostId: StoredId): Future[Seq[TriggerSet[Stored]]] = findByHostId(hostId, inherited = true)

  /**
    * get triggers without inherited
    */
  def getBelongingTriggers(hostId: StoredId): Future[Seq[TriggerSet[Stored]]] = findByHostId(hostId, inherited = false)

  private[this] def findByHostId(hostId: StoredId, inherited: Boolean): Future[Seq[TriggerSet[Stored]]] = {
    val params = Json.obj()
      .prop("hostids" -> hostId)
      .prop("inherited" -> inherited)
      .prop("expandExpression" -> true)
      .prop("selectTags" -> "extend")
      .prop("selectDependencies" -> "extend")
    api.requestAs[Seq[TriggerSet[Stored]]]("trigger.get", params)
  }

  def findByHostId(hostId: StoredId): Future[Seq[TriggerSet[Stored]]] = {
    val params = Json.obj()
      .prop("hostids" -> hostId)
      .prop("expandExpression" -> true)
      .prop("selectTags" -> "extend")
      .prop("selectDependencies" -> "extend")
    api.requestAs[Seq[TriggerSet[Stored]]]("trigger.get", params)
  }

  // trigger.create does not require template(host) id.
  def create(trigger: TriggerSet[NotStored]): Future[Report] = {
    val param = Json.toJson(trigger).as[JsObject] - "triggerid"
    api.requestSingleId("trigger.create", param, "triggerids")
      .map(id => Report.created(trigger.trigger.toStored(id)))
  }

  def update(triggerId: StoredId, trigger: TriggerSet[NotStored]): Future[Report] = {
    val param = Json.toJson(trigger.toStored(triggerId)).as[JsObject]
    api.requestSingleId("trigger.update", param, "triggerids")
      .map(id => Report.updated(trigger.trigger.toStored(id)))
  }

  def updateInherited(triggerId: StoredId, trigger: TriggerSet[NotStored]): Future[Report] = {
    // In zabbix 3.2.0 and later, the inherited trigger description and expression can not be updated
    val param = Json.toJson(trigger.toStored(triggerId)).as[JsObject] - "description" - "expression"
    api.requestSingleId("trigger.update", param, "triggerids")
      .map(id => Report.updated(trigger.trigger.toStored(id)))
  }

  def delete(triggers: Seq[Trigger[Stored]]): Future[Report] = {
    deleteEntities(api, triggers, "trigger.delete", "triggerids")
  }

  private[this] def findByHostIdAndTriggerNameAbsolutely(hostId: StoredId, triggerName: String): Future[TriggerSet[Stored]] = {
    val params = Json.obj()
      .prop("hostids" -> hostId)
      .filter("description", triggerName)
      .prop("expandExpression" -> true)
      .prop("selectTags" -> "extend")
      .prop("selectDependencies" -> "extend")
    api.requestAs[Seq[TriggerSet[Stored]]]("trigger.get", params).map { founds =>
      if (founds.isEmpty)
        throw NoSuchEntityException(s"No such dependet triggers: ${triggerName} ")
      else if (founds.length > 1)
        throw EntityDuplicated(s"Name of triggers has duplicate. name=${triggerName}")
      else
        founds.head
    }
  }

  def configToNotStored(template: Template[Stored], conf: TriggerConf): Future[TriggerSet[NotStored]] = {
    val dependenciesFuture = conf.dependencies.futureMap { depends =>
      Future.traverse(depends) { conf =>
        conf.template.futureMap(templateName => templateOp.findByHostnamesAbsolutely(Seq(templateName)).map(_.head.template.getStoredId))
          .map(_.getOrElse(template.getStoredId))
          .flatMap(templateId => findByHostIdAndTriggerNameAbsolutely(templateId, conf.name))
          .map(_.trigger.getStoredId)
      }
    }
    dependenciesFuture.map( dependencies =>
      TriggerSet(conf.trigger, dependencies, conf.tags)
    )
  }

  private[this] def createOrUpdate(hostId:  StoredId, constants: Seq[TriggerSet[NotStored]], belongings: Seq[TriggerSet[Stored]], inherited: Seq[TriggerSet[Stored]]): Future[Report] = {
    traverseOperations(constants) { triggerSet =>
      (belongings ++ inherited).find(_.trigger.description == triggerSet.trigger.description) match {
        case Some(stored) =>
          if (stored.shouldBeUpdated(triggerSet)) {
            if (belongings.exists(_.trigger.getStoredId == stored.trigger.getStoredId)) {
              update(stored.trigger.getStoredId, triggerSet)
            } else {
              // inherited trigger
              // In zabbix 3.2.0 and later, the inherited trigger description and expression can not be updated
              if (stored.trigger.expression != triggerSet.trigger.expression) {
                throw new UnsupportedOperation(s"The inherited trigger expression can not be updated.: trigger=${stored.trigger.description} expression=${triggerSet.trigger.expression}")
              }
              updateInherited(stored.trigger.getStoredId, triggerSet)
            }
          }
          else
            Future.successful(Report.empty())
        case None =>
          create(triggerSet)
      }
    }
  }

  def presentWithTemplate(templateName: String, configs: Seq[TriggerConf]): Future[Report] = {
    for {
      Seq(template) <- templateOp.findByHostnamesAbsolutely(Seq(templateName))
      constants <- Future.traverse(configs)(conf => configToNotStored(template.template, conf))
      inheritedItems <- getInheritedTriggers(template.template.getStoredId).map { s =>
        s.filter(i => configs.exists(_.trigger.description == i.trigger.description))
      }
      belongings <- getBelongingTriggers(template.template.getStoredId)
      updated <- createOrUpdate(template.template.getStoredId, constants, belongings, inheritedItems)
      deleted <- delete(belongings
        .filter(t => !configs.exists(_.trigger.description == t.trigger.description))
        .filter(t => !inheritedItems.contains(t.trigger.description))
        .map(_.trigger))
    } yield updated + deleted
  }

  def presentWithTemplate(triggerConfigs: Map[String, Seq[TriggerConf]]): Future[Report] = {
    traverseOperations(triggerConfigs) { case (template, configs) =>
      showStartInfo(logger, configs.length, s"triggers of temlate '$template'").flatMap { _ =>
        presentWithTemplate(template, configs)
      }
    }
  }

  def absentWithTemplate(triggerConfigs: Map[String, Seq[String]]): Future[Report] = {
    for {
      templates <- templateOp.findByHostnames(triggerConfigs.keys.toSeq)
      report <- traverseOperations(templates) { t =>
        getBelongingTriggers(t.template.getStoredId).flatMap { stored =>
          delete(stored.map(_.trigger).filter(s => triggerConfigs(t.template.host).contains(s.description)))
        }
      }
    } yield report
  }
}
