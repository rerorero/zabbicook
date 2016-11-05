package com.github.zabbicook.recipe

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.action.ActionConfig
import com.github.zabbicook.entity.host.{HostConf, HostGroup}
import com.github.zabbicook.entity.media.MediaType
import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.template.TemplateSettingsConf
import com.github.zabbicook.entity.user.{UserConfig, UserGroupConfig}

case class Recipe(
  mediaTypes: Option[Seq[MediaType[NotStored]]],
  hostGroups: Option[Seq[HostGroup[NotStored]]],
  hosts: Option[Seq[HostConf]],
  userGroups: Option[Seq[UserGroupConfig]],
  users: Option[Seq[UserConfig]],
  actions: Option[Seq[ActionConfig]],
  templates: Option[Seq[TemplateSettingsConf]]
)

object Recipe extends EntityCompanionMetaHelper {
  val meta = entity("root")(
    arrayOf("mediaTypes")(MediaType.optional("mediaTypes")),
    arrayOf("hostGroups")(HostGroup.optional("hostGroups")),
    arrayOf("hosts")(HostConf.optional("hosts")),
    arrayOf("userGroups")(UserGroupConfig.optional("userGroups")),
    arrayOf("users")(UserConfig.optional("users")),
    arrayOf("actions")(ActionConfig.optional("actions")),
    arrayOf("templates")(TemplateSettingsConf.optional("templates"))
  ) _
}
