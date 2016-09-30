package com.github.zabbicook.recipe

import com.github.zabbicook.entity.HostGroup
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import com.github.zabbicook.operation.{UserConfig, UserGroupConfig}

case class Recipe(
  hostGroups: Seq[HostGroup],
  userGroupsAndPermissions: Set[UserGroupConfig],
  users: Set[UserConfig]
)

object Recipe {
  implicit val hoconReads: HoconReads[Recipe] = {
    for {
      hostGroups <- optional[Seq[String]]("hostGroups").map(_.map(_.map(HostGroup.fromString)))
      userGroups <- optionalMapToSet[UserGroupConfig]("userGroups", "name")
      users <- optionalMapToSet[UserConfig]("users", "alias")
    } yield {
      Recipe(
        hostGroups = hostGroups.getOrElse(Seq()),
        userGroupsAndPermissions = userGroups.getOrElse(Set()),
        users = users.getOrElse(Set())
      )
    }
  }
}
