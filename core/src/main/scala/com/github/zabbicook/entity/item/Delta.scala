package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.prop._

sealed abstract class Delta(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object Delta extends IntEnumPropCompanion[Delta] {
  override val values: Set[Delta] = Set(
    AsIs,SpeedPerSec,SimpleChange,unknown
  )
  override val description = "Value that will be stored."
  case object AsIs extends Delta(0, "(default) as is")
  case object SpeedPerSec extends Delta(1, "Delta, speed per second.")
  case object SimpleChange extends Delta(2, "Delta, simple change")
  case object unknown extends Delta(-1, "unknown")
}
