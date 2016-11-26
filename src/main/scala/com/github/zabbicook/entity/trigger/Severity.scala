package com.github.zabbicook.entity.trigger

import com.github.zabbicook.entity.prop.{EnumProp, IntEnumPropCompanion, IntProp}

sealed abstract class Severity(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object Severity extends IntEnumPropCompanion[Severity] {
  override val values: Seq[Severity] = Seq(classified,information,warning,average,high,disaster,unknown)
  override val description: String = "Severity of the trigger."
  case object classified extends Severity(0, "(default) not classified")
  case object information extends Severity(1, "information")
  case object warning extends Severity(2, "warning")
  case object average extends Severity(3, "average")
  case object high extends Severity(4, "high")
  case object disaster extends Severity(5,"disaster")
  case object unknown extends Severity(-1, "unknown")
}
