package com.github.zabbicook.entity.user

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop._
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

sealed abstract class Permission(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object Permission extends IntEnumPropCompanion[Permission] {
  override val values: Set[Permission] = Set(denied,readOnly,readWrite,unknown)
  override val description: String = "Access level to the host group."
  case object denied extends Permission(0, "access denied")
  case object readOnly extends Permission(2, "read-only access")
  case object readWrite extends Permission(3, "read-write access")
  case object unknown extends Permission(-1, "unknown")
}

case class UserGroupPermission[S <: EntityState](
  id: EntityId,
  permission: Permission
) extends Entity[S] {
  def toStored(_id: StoredId): UserGroupPermission[Stored] = copy(id = _id)
}

object UserGroupPermission extends EntityCompanionMetaHelper {
  implicit val format: Format[UserGroupPermission[Stored]] = Json.format[UserGroupPermission[Stored]]

  implicit val format2: Format[UserGroupPermission[NotStored]] = Json.format[UserGroupPermission[NotStored]]

  val meta = entity("The permission object.")(
    readOnly("id"),
    Permission.meta("permission")("permission")
  ) _
}
