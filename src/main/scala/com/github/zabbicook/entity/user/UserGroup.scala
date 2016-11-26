package com.github.zabbicook.entity.user

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop._
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, JsObject, Json}

sealed abstract class GuiAccess(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object GuiAccess extends IntEnumPropCompanion[GuiAccess] {
  override val values: Seq[GuiAccess] = Seq(default,internal,disable,unknown)
  override val description: String = "Frontend authentication method of the users in the group."
  case object default extends GuiAccess(0, "(default) use the system default authentication method")
  case object internal extends GuiAccess(1, "use internal authentication")
  case object disable extends GuiAccess(2, "disable access to the frontend")
  case object unknown extends GuiAccess(-1, "unknown")
}

case class UserGroup[S <: EntityState](
  usrgrpid: EntityId = NotStoredId,   // readonly
  name: String,                     // required
  debug_mode: Option[EnabledEnum] = None,
  gui_access: Option[GuiAccess] = None,
  users_status: Option[EnabledEnumZeroPositive] = None
) extends Entity[S] {
  override protected[this] val id: EntityId = usrgrpid

  def toStored(id: StoredId): UserGroup[Stored] = copy(usrgrpid = id)

  def toJsonForUpdate[T >: S <: NotStored](_id: StoredId): JsObject = {
    Json.toJson(copy(usrgrpid = _id).asInstanceOf[UserGroup[Stored]]).as[JsObject]
  }

  def shouldBeUpdated[T >: S <: Stored](other: UserGroup[NotStored]): Boolean = {
    require(name == other.name)
    shouldBeUpdated(debug_mode, other.debug_mode)
    shouldBeUpdated(gui_access, other.gui_access)
    shouldBeUpdated(users_status, other.users_status)
  }
}

object UserGroup extends EntityCompanionMetaHelper {
  type DebugMode = EnabledEnum
  type State = EnabledEnumZeroPositive

  implicit val format: Format[UserGroup[Stored]] = Json.format[UserGroup[Stored]]

  implicit val format2: Format[UserGroup[NotStored]] = Json.format[UserGroup[NotStored]]

  override val meta = entity("User group object.")(
    readOnly("usrgrpid"),
    value("name")("name")("(required) Name of the user group."),
    EnabledEnum.metaWithDesc("debug_mode")("debug", "debugMode")("Whether debug mode is enabled or disabled."),
    GuiAccess.meta("gui_access")("guiAccess","gui"),
    EnabledEnumZeroPositive.metaWithDesc("users_status")("enabled", "usersStatus")("Whether the user group is enabled or disabled.")
  ) _
}
