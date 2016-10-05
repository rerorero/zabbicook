package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.prop.{IntProp, IntEnumDescribedWithString, IntEnumDescribedWithStringCompanion}
import play.api.libs.json.{Format, Json}

sealed abstract class Permission(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = Permission.validate(this)
}

object Permission extends IntEnumDescribedWithStringCompanion[Permission] {
  override val all: Set[Permission] = Set(denied,readOnly,readWrite,unknown)
  case object denied extends Permission(0)
  case object readOnly extends Permission(2)
  case object readWrite extends Permission(3)
  case object unknown extends Permission(-1)
}

case class UserGroupPermission[S <: EntityState](
  id: EntityId,
  permission: Permission
) extends Entity[S] {
  def toStored(_id: StoredId): UserGroupPermission[Stored] = copy(id = _id)
}

object UserGroupPermission {
  implicit val format: Format[UserGroupPermission[Stored]] = Json.format[UserGroupPermission[Stored]]

  implicit val format2: Format[UserGroupPermission[NotStored]] = Json.format[UserGroupPermission[NotStored]]
}
