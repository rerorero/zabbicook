package com.github.zabbicook.recipe

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.HostGroup
import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.operation.TemplateSettings.TemplateSettingsConf
import com.github.zabbicook.operation.{TemplateSettings, UserConfig, UserGroupConfig}

case class Recipe(
  hostGroups: Option[Seq[HostGroup[NotStored]]],
  userGroups: Option[Seq[UserGroupConfig]],
  users: Option[Seq[UserConfig]],
  templates: Option[Seq[TemplateSettingsConf]]
)

object Recipe extends EntityCompanionMetaHelper {
  val meta = entity("root")(
    arrayOf("hostGroups")(HostGroup.optional("hostGroups")),
    arrayOf("userGroups")(UserGroupConfig.optional("userGroups")),
    arrayOf("users")(UserConfig.optional("users")),
    arrayOf("templates")(TemplateSettings.optional("templates"))
  ) _
}
