package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.prop.{EnumProp, IntEnumPropCompanion, IntProp}

sealed abstract class EventSource(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object EventSource extends IntEnumPropCompanion[EventSource] {
  override val values: Seq[EventSource] = Seq(trigger,discovery,registration,internal,unknown)
  override val description: String = "Type of the event."
  case object trigger extends EventSource(0, "event created by a trigger")
  case object discovery extends EventSource(1, "event created by a discovery rule")
  case object registration extends EventSource(2,"event created by active agent auto-registration")
  case object internal extends EventSource(3,"internal event")
  case object unknown extends EventSource(-1, "unknown")
}
