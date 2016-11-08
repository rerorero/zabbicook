package com.github.zabbicook.entity.trigger

import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import play.api.libs.json.{Format, Json}
import com.github.zabbicook.entity.prop.Meta._

case class TriggerTag(
  tag: String,
  value: String
)

object TriggerTag extends EntityCompanionMetaHelper {
  implicit val format: Format[TriggerTag] = Json.format[TriggerTag]

  override val meta = entity("Trigger tags.")(
    value("tag")("tag")("(required) Tag name."),
    value("value")("value")("(required) Tag value.")
  ) _
}
