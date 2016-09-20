package com.github.zabbicook.entity

import play.api.libs.json._

trait NumberEnumProp {
  def value: NumProp
}

trait NumberEnumPropCompanion[T <: NumberEnumProp] {
  def all: Set[T]
  def unknown: T

  implicit val format: Format[T] = Format(
    implicitly[Reads[NumProp]].map(n => all.find(_.value == n).getOrElse(unknown)),
    Writes(t => JsNumber(t.value.value))
  )
}
