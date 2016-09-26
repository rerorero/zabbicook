package com.github.zabbicook.entity

import play.api.libs.json._

trait NumberEnumProp extends EnumProp {
  def value: NumProp
}

trait NumberEnumPropCompanion[T <: NumberEnumProp] extends EnumCompanion[T] {
  implicit val format: Format[T] = Format(
    implicitly[Reads[NumProp]].map(n => all.find(_.value == n).getOrElse(unknown)),
    Writes(t => JsNumber(t.value.value))
  )

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def numberToEnum(n: Int): T = {
    all.find(_.value == n).getOrElse(unknown)
  }

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def numberToEnum(n: Option[Int]): Option[T] = n.map(numberToEnum)
}

trait NumberEnumDescribedWithString extends NumberEnumProp

trait NumberEnumDescribedWithStringCompanion[T <: NumberEnumDescribedWithString] extends NumberEnumPropCompanion[T] {

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def stringToEnum(s: String): T = {
    all.find(_.toString == s).getOrElse(unknown)
  }

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def stringToEnum(s: Option[String]): Option[T] = s.map(stringToEnum)
}
