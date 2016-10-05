package com.github.zabbicook.entity.item

import com.github.zabbicook.entity._
import com.github.zabbicook.entity.prop.{IntEnumDescribedWithString, IntEnumDescribedWithStringCompanion, IntProp}

sealed abstract class AuthType(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = AuthType.validate(this)
}

object AuthType extends IntEnumDescribedWithStringCompanion[AuthType] {
  override val all: Set[AuthType] = Set(
    password,publicKey,unknown
  )
  case object password extends AuthType(0)
  case object publicKey extends AuthType(1)
  case object unknown extends AuthType(-1)
}

