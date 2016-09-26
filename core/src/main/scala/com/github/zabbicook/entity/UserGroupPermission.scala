package com.github.zabbicook.entity

import com.github.zabbicook.entity.HostGroup.HostGroupId
import play.api.libs.json.{Format, Json}

sealed abstract class Permission(val value: NumProp) extends NumberEnumDescribedWithString {
  override def validate(): ValidationResult = Permission.validate(this)
}

object Permission extends NumberEnumDescribedWithStringCompanion[Permission] {
  override val all: Set[Permission] = Set(denied,readOnly,readWrite,unknown)
  case object denied extends Permission(0)
  case object readOnly extends Permission(2)
  case object readWrite extends Permission(3)
  case object unknown extends Permission(-1)
}

case class UserGroupPermission(
  id: HostGroupId,
  permission: Permission
) extends Entity

object UserGroupPermission {
  implicit val format: Format[UserGroupPermission] = Json.format[UserGroupPermission]
}
