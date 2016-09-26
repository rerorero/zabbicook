package com.github.zabbicook.entity

import com.github.zabbicook.entity.UserGroup.UserGroupId
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{Format, Json}

sealed abstract class GuiAccess(val value: NumProp) extends NumberEnumProp {
  override def validate(): ValidationResult = GuiAccess.validate(this)
}

object GuiAccess extends NumberEnumPropCompanion[GuiAccess] {
  override val all: Set[GuiAccess] = Set(default,internal,disable,unknown)
  case object default extends GuiAccess(0)
  case object internal extends GuiAccess(1)
  case object disable extends GuiAccess(2)
  case object unknown extends GuiAccess(-1)
}

case class UserGroup(
  usrgrpid: Option[UserGroupId] = None,   // readonly
  name: String,                     // required
  debug_mode: Option[EnabledEnum] = None,
  gui_access: Option[GuiAccess] = None,
  users_status: Option[EnabledEnumZeroPositive] = None
) extends Entity {

  def removeReadOnly: UserGroup = copy(usrgrpid = None)

  def shouldBeUpdated(other: UserGroup): Boolean = {
    require(name == other.name)
    shouldBeUpdated(debug_mode, other.debug_mode)
    shouldBeUpdated(gui_access, other.gui_access)
    shouldBeUpdated(users_status, other.users_status)
  }
}

object UserGroup {
  type UserGroupId = String
  type DebugMode = EnabledEnum
  type State = EnabledEnumZeroPositive

  implicit val format: Format[UserGroup] = Json.format[UserGroup]

  implicit val hoconReads: HoconReads[UserGroup] = {
    for {
      name <- required[String]("name")
      debugMode <- optional[DebugMode]("debugMode")
      userStatus <- optional[State]("enabled")
    } yield {
      UserGroup(
        name = name,
        debug_mode = debugMode,
        users_status = userStatus
      )
    }
  }
}
