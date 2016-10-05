package com.github.zabbicook.recipe

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.HostGroup
import com.github.zabbicook.hocon.HoconReads._
import com.github.zabbicook.hocon.{HoconReads, TemplateSection}
import com.github.zabbicook.operation.{UserConfig, UserGroupConfig}

case class Recipe(
  hostGroups: Seq[HostGroup[NotStored]],
  userGroupsAndPermissions: Seq[UserGroupConfig],
  users: Seq[UserConfig],
  templates: Seq[TemplateSection]
)

object Recipe {
  implicit val hoconReads: HoconReads[Recipe] = {
    for {
      hostGroups <- optional[Seq[String]]("hostGroups").map(_.map(_.map(HostGroup.fromString)))
      userGroups <- optional[Seq[UserGroupConfig]]("userGroups")
      users <- optional[Seq[UserConfig]]("users")
      templates <- optional[Seq[TemplateSection]]("templates")
    } yield {
      Recipe(
        hostGroups = hostGroups.getOrElse(Seq()),
        userGroupsAndPermissions = userGroups.getOrElse(Seq()),
        users = users.getOrElse(Seq()),
        templates = templates.getOrElse(Seq())
      )
    }
  }
}
