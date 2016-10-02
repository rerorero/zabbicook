package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.{NumProp, NumberEnumDescribedWithString, NumberEnumDescribedWithStringCompanion, ValidationResult}

sealed abstract class DataType(val value: NumProp) extends NumberEnumDescribedWithString {
  override def validate(): ValidationResult = DataType.validate(this)
}

object DataType extends NumberEnumDescribedWithStringCompanion[DataType] {
  override val all: Set[DataType] = Set(
    decimal,octal,hexadecimal,boolean,unknown
  )
  case object decimal extends DataType(0)
  case object octal extends DataType(1)
  case object hexadecimal extends DataType(2)
  case object boolean extends DataType(3)
  case object unknown extends DataType(-1)
}
