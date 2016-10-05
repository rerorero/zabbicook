package com.github.zabbicook.entity.item

import com.github.zabbicook.entity._
import com.github.zabbicook.entity.prop.{IntEnumDescribedWithString, IntEnumDescribedWithStringCompanion, IntProp}

sealed abstract class DataType(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = DataType.validate(this)
}

object DataType extends IntEnumDescribedWithStringCompanion[DataType] {
  override val all: Set[DataType] = Set(
    decimal,octal,hexadecimal,boolean,unknown
  )
  case object decimal extends DataType(0)
  case object octal extends DataType(1)
  case object hexadecimal extends DataType(2)
  case object boolean extends DataType(3)
  case object unknown extends DataType(-1)
}
