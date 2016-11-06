package com.github.zabbicook.entity.trigger

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop.{EntityCompanionMetaHelper, _}
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

/**
  * @see [[https://www.zabbix.com/documentation/3.2/manual/api/reference/trigger/object]]
  */
case class Trigger[S <: EntityState] (
  triggerid: EntityId = NotStoredId,
  description: String,
  expression: String,
  comments: Option[String] = None,
  priority: Option[Severity] = None,
  status: Option[EnabledEnumZeroPositive] = None,
  `type`: Option[EventGenerationMode] = None,
  url: Option[String] = None,
  recovery_mode: Option[RecoveryMode] = None,  // TODO: version >= 3.2 validation
  recovery_expression: Option[String] = None,  // version >= 3.2
  correlation_mode: Option[CorrelationMode] = None,  // version >= 3.2
  correlation_tag: Option[String] = None,    // version >= 3.2
  manual_close: Option[EnabledEnum] = None  // version >= 3.2
) extends Entity[S] {
  override protected[this] def id: EntityId = triggerid

  def toStored(id: StoredId): Trigger[Stored] = copy(triggerid = id)

  def shouldBeUpdated[T >: S <: Stored](constant: Trigger[NotStored]): Boolean = {
    require(description == constant.description)

    expression != constant.expression ||
      shouldBeUpdated(comments, constant.comments) ||
      shouldBeUpdated(priority, constant.priority) ||
      shouldBeUpdated(status, constant.status) ||
      shouldBeUpdated(`type`, constant.`type`) ||
      shouldBeUpdated(url, constant.url) ||
      shouldBeUpdated(recovery_mode, constant.recovery_mode) ||
      shouldBeUpdated(recovery_expression, constant.recovery_expression) ||
      shouldBeUpdated(correlation_mode, constant.correlation_mode) ||
      shouldBeUpdated(correlation_tag, constant.correlation_tag) ||
      shouldBeUpdated(manual_close, constant.manual_close)
  }
}

object Trigger extends EntityCompanionMetaHelper {
  implicit val format: Format[Trigger[Stored]] = Json.format[Trigger[Stored]]

  implicit val format2: Format[Trigger[NotStored]] = Json.format[Trigger[NotStored]]

  override val meta = entity("Trigger object.")(
    readOnly("triggerid"),
    value("description")("name")("(required) Name of the trigger."),
    value("expression")("expression")("(required)	Reduced trigger expression."),
    value("comments")("description","comments")("Additional comments to the trigger."),
    Severity.meta("priority")("severity","priority"),
    EnabledEnumZeroPositive.metaWithDesc("status")("enabled")("Whether the trigger is enabled or disabled."),
    EventGenerationMode.meta("type")("eventGenerationMode","multipleEvents"),
    value("url")("url")("URL associated with the trigger."),
    RecoveryMode.meta("recovery_mode")("okEventGeneration","recoveryMode"),
    value("recovery_expression")("recoveryExpression")("Reduced trigger recovery expression. (available Zabbix version >= 3.2)"),
    CorrelationMode.meta("correlation_mode")("okEventCloses"),
    value("correlation_tag")("tagForMatching")("Tag for matching. (available in Zabbix version >= 3.2.0)"),
    EnabledEnum.metaWithDesc("manual_close")("manualClose","allowManualClose")("Allow manual close.")
  ) _
}
