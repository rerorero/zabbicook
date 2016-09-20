package com.github.zabbicook.entity

/**
  * Zabbix represents two patterns of 'enabled' flag... Be careful!
  */

sealed abstract class EnabledEnum(val value: NumProp) extends NumberEnumProp

object EnabledEnum extends NumberEnumPropCompanion[EnabledEnum] {
  val all: Set[EnabledEnum] = Set(disabled, enabled, unknown)
  case object disabled extends EnabledEnum(0)
  case object enabled extends EnabledEnum(1)
  case object unknown extends EnabledEnum(-1)
}

sealed abstract class EnabledEnumZeroPositive(val value: NumProp) extends NumberEnumProp

object EnabledEnumZeroPositive extends NumberEnumPropCompanion[EnabledEnumZeroPositive] {
  val all: Set[EnabledEnumZeroPositive] = Set(disabled, enabled, unknown)
  case object enabled extends EnabledEnumZeroPositive(0)
  case object disabled extends EnabledEnumZeroPositive(1)
  case object unknown extends EnabledEnumZeroPositive(-1)
}
