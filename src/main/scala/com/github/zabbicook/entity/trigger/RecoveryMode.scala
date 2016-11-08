package com.github.zabbicook.entity.trigger

import com.github.zabbicook.entity.prop.{EnumProp, IntEnumPropCompanion, IntProp}

sealed abstract class RecoveryMode(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object RecoveryMode extends IntEnumPropCompanion[RecoveryMode] {
  override val values: Set[RecoveryMode] = Set(expression, recoveryExpression, none,unknown)
  override val description: String = "OK event generation mode. (Available in Zabbix Version >= 3.2.0.)"
  case object expression extends RecoveryMode(0, "(default) Expression")
  case object recoveryExpression extends RecoveryMode(1, "Recovery expression ('recoveryExpression' required)") // TODO validation
  case object none extends RecoveryMode(2, "None")
  case object unknown extends RecoveryMode(-1, "unknown")
}
