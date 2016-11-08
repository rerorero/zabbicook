package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.prop.{EnumProp, IntEnumPropCompanion, IntProp}

sealed abstract class OperationEvalType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object OperationEvalType extends IntEnumPropCompanion[OperationEvalType] {
  override val values: Set[OperationEvalType] = Set(AndOr,And,Or,unknown)
  override val description: String = "Operation condition evaluation method."
  case object AndOr extends OperationEvalType(0, "(default) AND / OR")
  case object And extends OperationEvalType(1, "AND")
  case object Or extends OperationEvalType(2, "OR")
  case object unknown extends OperationEvalType(-1, "unknown")
}
