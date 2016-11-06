package com.github.zabbicook.entity.trigger

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import com.github.zabbicook.entity.prop.Meta._
import play.api.libs.json.{Format, Json}

case class TriggerDependenciesConf(
  template: Option[String],
  name: String
)

object TriggerDependenciesConf extends EntityCompanionMetaHelper {
  implicit val format: Format[TriggerDependenciesConf] = Json.format[TriggerDependenciesConf]

  override val meta = entity("Dependent triggers.")(
    value("template")("template")(
      """Template name to which the trigger belongs.
        |(default) The template to which this trigger belongs. """.stripMargin),
    value("name")("name")("(required) Trigger name.")
  ) _
}

case class TriggerConf(
  trigger: Trigger[NotStored],
  dependencies: Option[Seq[TriggerDependenciesConf]] = None,
  tags: Option[Seq[TriggerTag]] = None
)

object TriggerConf extends EntityCompanionMetaHelper {
  implicit val format: Format[TriggerConf] = Json.format[TriggerConf]

  override val meta = entity("Trigger setting.")(
    Trigger.required("trigger"),
    arrayOf("dependencies")(TriggerDependenciesConf.optional("dependencies")),
    arrayOf("tags")(TriggerTag.optional("tags"))
  ) _
}
