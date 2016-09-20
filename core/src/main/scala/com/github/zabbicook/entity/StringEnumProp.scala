package com.github.zabbicook.entity

import play.api.libs.json._

trait StringEnumProp {
  def value: String
}

trait StringEnumCompanion[T <: StringEnumProp] {
  def all: Set[T]
  def unknown: T
  implicit val format: Format[T] = Format(
    Reads.StringReads.map(n => all.find(_.value == n).getOrElse(unknown)),
    Writes(v => JsString(v.value))
  )
}

