package com.github.zabbicook.entity.item

import com.github.zabbicook.entity._
import com.github.zabbicook.entity.prop.{IntEnumDescribedWithString, IntEnumDescribedWithStringCompanion, IntProp}

sealed abstract class Delta(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = Delta.validate(this)
}

object Delta extends IntEnumDescribedWithStringCompanion[Delta] {
  override val all: Set[Delta] = Set(
    AsIs,SpeedPerSec,SimpleChange,unknown
  )
  case object AsIs extends Delta(0)
  case object SpeedPerSec extends Delta(1)
  case object SimpleChange extends Delta(2)
  case object unknown extends Delta(-1)
}
