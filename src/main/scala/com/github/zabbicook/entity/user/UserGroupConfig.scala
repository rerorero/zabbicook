package com.github.zabbicook.entity.user

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import com.github.zabbicook.entity.prop.Meta._

case class PermissionsOfHosts(
  host: String,
  permission: Permission
)

object PermissionsOfHosts extends EntityCompanionMetaHelper {
  val meta = entity("Permission of ths host group.")(
    value("host")("hostgroup", "hostGroup")("Name of the host group"),
    Permission.meta("permission")("permission")
  ) _
}

/**
  * @param userGroup User group
  * @param permissions host group and a permission which describes the access level to the host.
  *                    The specified host groups must be presented before calling present() function.
  */
case class UserGroupConfig(
  userGroup: UserGroup[NotStored],
  permissions: Seq[PermissionsOfHosts]
) {
  def permissionsOfHostGroup(hostgroup: String): Option[PermissionsOfHosts] = {
    permissions.find(_.host == hostgroup)
  }
}

object UserGroupConfig extends EntityCompanionMetaHelper {
  val meta = entity("Permission of a user group.")(
    UserGroup.required("userGroup"),
    arrayOf("permissions")(PermissionsOfHosts.required("permissions"))
  ) _
}
