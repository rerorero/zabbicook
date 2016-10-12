package com.github.zabbicook.entity.user

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import com.github.zabbicook.entity.prop.Meta._

/**
  * @param user user object
  * @param groupNames names of groups to which the user belongs
  * @param password password (it is used only if the user does not exist yet)
  * TODO: Do we need to separate the password from here?
  */
case class UserConfig(user: User[NotStored], groupNames: Seq[String], password: String)

object UserConfig extends EntityCompanionMetaHelper {
  val meta = entity("User settings.")(
    User.required("user"),
    array("groupNames")("groups")("(required) User groups to add the user to."),
    value("password")("password")("(required) User's password.")
  ) _
}
