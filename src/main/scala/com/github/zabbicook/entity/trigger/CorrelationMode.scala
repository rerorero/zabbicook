package com.github.zabbicook.entity.trigger

import com.github.zabbicook.entity.prop.{EnumProp, IntEnumPropCompanion, IntProp}

sealed abstract class CorrelationMode(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object CorrelationMode extends IntEnumPropCompanion[CorrelationMode] {
  override val values: Set[CorrelationMode] = Set(all,tagMatched,unknown)
  override val description: String = "OK event closes. (available in Zabbix Version >= 3.2.0.)"
  case object all extends CorrelationMode(0, "(default) All problems")
  case object tagMatched extends CorrelationMode(1, "All problems if tag values match.")
  case object unknown extends CorrelationMode(-1, "unknown")
}
