package com.github.zabbicook.entity

import play.api.libs.json._

trait StringEnumProp extends EnumProp {
  def value: String
}

trait StringEnumCompanion[T <: StringEnumProp] extends EnumCompanion[T] {
  implicit val format: Format[T] = Format(
    Reads.StringReads.map(n => all.find(_.value == n).getOrElse(unknown)),
    Writes(v => JsString(v.value))
  )

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def stringToEnum(s: String): T = {
    all.find(_.toString == s).getOrElse(unknown)
  }

  // required to resolve HoconReads.ConfigFunc[T] implicitly
  implicit def stringToEnum(s: Option[String]): Option[T] = s.map(stringToEnum)
}

