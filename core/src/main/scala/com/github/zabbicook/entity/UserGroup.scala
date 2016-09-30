package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{Format, JsObject, Json}

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

object UserGroup {
  type DebugMode = EnabledEnum
  type State = EnabledEnumZeroPositive

  implicit val format: Format[UserGroup[Stored]] = Json.format[UserGroup[Stored]]

  implicit val format2: Format[UserGroup[NotStored]] = Json.format[UserGroup[NotStored]]

  implicit val hoconReads: HoconReads[UserGroup[NotStored]] = {
    val reads = for {
      name <- required[String]("name")
      debugMode <- optional[DebugMode]("debugMode")
      userStatus <- optional[State]("enabled")
    } yield {
      UserGroup[NotStored](
        name = name,
        debug_mode = debugMode,
        users_status = userStatus
      )
    }
    reads.withAcceptableKeys("name", "debugMode", "enabled")
  }
}
