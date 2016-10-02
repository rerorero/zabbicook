package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.{NumProp, NumberEnumDescribedWithString, NumberEnumDescribedWithStringCompanion, ValidationResult}

sealed abstract class ValueType(val value: NumProp) extends NumberEnumDescribedWithString {
  override def validate(): ValidationResult = ValueType.validate(this)
}

object ValueType extends NumberEnumDescribedWithStringCompanion[ValueType] {
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
