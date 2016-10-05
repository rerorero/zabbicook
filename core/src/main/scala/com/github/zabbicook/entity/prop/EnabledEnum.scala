package com.github.zabbicook.entity.prop

import com.github.zabbicook.entity.ValidationResult

/**
  * Zabbix represents two patterns of 'enabled' flag... Be careful!
  */

sealed abstract class EnabledEnum(val value: IntProp) extends IntEnumProp {
  override def validate(): ValidationResult = EnabledEnum.validate(this)
}

object EnabledEnum extends IntEnumPropCompanion[EnabledEnum] {
  val all: Set[EnabledEnum] = Set(disabled, enabled, unknown)
  case object disabled extends EnabledEnum(0)
  case object enabled extends EnabledEnum(1)
  case object unknown extends EnabledEnum(-1)

  implicit def boolean2enum(b: Boolean): EnabledEnum = if (b) enabled else disabled
  implicit def boolean2enum(b: Option[Boolean]): Option[EnabledEnum] = b.map(boolean2enum)
}

sealed abstract class EnabledEnumZeroPositive(val value: IntProp) extends IntEnumProp {
  override def validate(): ValidationResult = EnabledEnumZeroPositive.validate(this)
}

object EnabledEnumZeroPositive extends IntEnumPropCompanion[EnabledEnumZeroPositive] {
  val all: Set[EnabledEnumZeroPositive] = Set(disabled, enabled, unknown)
  case object enabled extends EnabledEnumZeroPositive(0)
  case object disabled extends EnabledEnumZeroPositive(1)
  case object unknown extends EnabledEnumZeroPositive(-1)

  implicit def boolean2enum(b: Boolean): EnabledEnumZeroPositive = if (b) enabled else disabled
  implicit def boolean2enum(b: Option[Boolean]): Option[EnabledEnumZeroPositive] = b.map(boolean2enum)
}
