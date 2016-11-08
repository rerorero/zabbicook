package com.github.zabbicook.entity.trigger

import com.github.zabbicook.entity.prop.{EnumProp, IntEnumPropCompanion, IntProp}

sealed abstract class EventGenerationMode(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object EventGenerationMode extends IntEnumPropCompanion[EventGenerationMode] {
  override val values: Set[EventGenerationMode] = Set(single,multiple,unknown)
  override val description: String = "Whether the trigger can generate multiple problem events."
  case object single extends EventGenerationMode(0, "(default) do not generate multiple events.")
  case object multiple extends EventGenerationMode(1, "generate multiple events.")
  case object unknown extends EventGenerationMode(-1, "unknown")
}
