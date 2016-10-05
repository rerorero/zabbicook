package com.github.zabbicook.entity.prop

import play.api.libs.json._

trait IntEnumProp extends EnumProp {
  def value: IntProp
}

trait IntEnumPropCompanion[T <: IntEnumProp] extends EnumCompanion[T] {
  implicit val format: Format[T] = Format(
    implicitly[Reads[IntProp]].map(n => all.find(_.value == n).getOrElse(unknown)),
    Writes(t => JsNumber(t.value.value))
  )

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def numberToEnum(n: Int): T = {
    all.find(_.value == n).getOrElse(unknown)
  }

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def numberToEnum(n: Option[Int]): Option[T] = n.map(numberToEnum)
}

trait IntEnumDescribedWithString extends IntEnumProp

trait IntEnumDescribedWithStringCompanion[T <: IntEnumDescribedWithString] extends IntEnumPropCompanion[T] {

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def stringToEnum(s: String): T = {
    all.find(_.toString == s).getOrElse(unknown)
  }

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def stringToEnum(s: Option[String]): Option[T] = s.map(stringToEnum)
}
