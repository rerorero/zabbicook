package com.github.zabbicook.entity.item

import com.github.zabbicook.entity._
import com.github.zabbicook.entity.prop.{IntEnumDescribedWithString, IntEnumDescribedWithStringCompanion, IntProp}

sealed abstract class ValueType(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = ValueType.validate(this)
}

object ValueType extends IntEnumDescribedWithStringCompanion[ValueType] {
  override val all: Set[ValueType] = Set(
    float,character,log,unsigned,text,unknown
  )
  case object float extends ValueType(0)
  case object character extends ValueType(1)
  case object log extends ValueType(2)
  case object unsigned extends ValueType(3)
  case object text extends ValueType(4)
  case object unknown extends ValueType(-1)
}
