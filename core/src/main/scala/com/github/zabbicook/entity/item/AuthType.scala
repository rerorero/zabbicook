package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.{NumProp, NumberEnumDescribedWithString, NumberEnumDescribedWithStringCompanion, ValidationResult}

sealed abstract class AuthType(val value: NumProp) extends NumberEnumDescribedWithString {
  override def validate(): ValidationResult = AuthType.validate(this)
}

object AuthType extends NumberEnumDescribedWithStringCompanion[AuthType] {
  override val all: Set[AuthType] = Set(
    password,publicKey,unknown
  )
  case object password extends AuthType(0)
  case object publicKey extends AuthType(1)
  case object unknown extends AuthType(-1)
}

