package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.prop._

sealed abstract class AuthType(val zabbixValue: IntProp, val desc: String) extends EnumProp2[IntProp]

object AuthType extends IntEnumProp2Companion[AuthType] {
  override val values: Set[AuthType] = Set(
    password,publicKey,unknown
  )
  override val description: String = "SSH authentication method. Used only by SSH agent items."
  case object password extends AuthType(0, "(default) password")
  case object publicKey extends AuthType(1, "public key")
  case object unknown extends AuthType(-1, "unknown")
}

