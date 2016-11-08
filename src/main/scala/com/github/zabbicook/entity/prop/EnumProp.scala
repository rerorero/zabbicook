package com.github.zabbicook.entity.prop

import play.api.libs.json._

trait EnumProp[V] {
  def zabbixValue: V
  def desc: String
}

trait StringToEnumProp[V, T <: EnumProp[V]] {
  def convert(s: String): T
}

trait EnumPropCompanion[V, T <: EnumProp[V]] {
  def values: Set[T]

  def description: String

  def unknown: T

  def possibleValues: Set[T] = values - unknown

  def meta(name: String)(aliases: String*): EnumMeta =
    Meta.enum(name, possibleValues)(aliases:_*)(description)

  def metaWithDesc(name: String)(aliases: String*)(overrideDescription: String): EnumMeta =
    Meta.enum(name, possibleValues)(aliases:_*)(overrideDescription)

  implicit val StringToEnumProp: StringToEnumProp[V, T] = new StringToEnumProp[V, T] {
    override def convert(s: String): T = possibleValues.find(_.toString == s).getOrElse(unknown)
  }
}

trait StringEnumPropCompanion[T <: EnumProp[String]] extends EnumPropCompanion[String, T] {
  implicit val format: Format[T] = Format(
    Reads.StringReads.map(n => possibleValues.find(_.zabbixValue == n).getOrElse(unknown)),
    Writes(v => JsString(v.zabbixValue))
  )
}

trait IntEnumPropCompanion[T <: EnumProp[IntProp]] extends EnumPropCompanion[IntProp, T] {
  implicit val format: Format[T] = Format(
    implicitly[Reads[IntProp]].map(n => possibleValues.find(_.zabbixValue.value == n.value).getOrElse(unknown)),
    Writes(v => JsNumber(v.zabbixValue.value))
  )
}

/**
  * Zabbix represents two patterns of 'enabled' flag... Be careful!
  */
sealed abstract class EnabledEnum(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object EnabledEnum extends IntEnumPropCompanion[EnabledEnum] {
  override val values: Set[EnabledEnum] = Set(`false`, `true`, unknown)
  override val description: String = "Enabled status"
  case object `false` extends EnabledEnum(0, "Disable")
  case object `true` extends EnabledEnum(1, "Enable")
  case object unknown extends EnabledEnum(-1, "Unknown")
  val enabled = `true`
  val disabled = `false`

  implicit def boolean2enum(b: Boolean): EnabledEnum = if (b) enabled else disabled
  implicit def boolean2enum(b: Option[Boolean]): Option[EnabledEnum] = b.map(boolean2enum)
}

sealed abstract class EnabledEnumZeroPositive(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object EnabledEnumZeroPositive extends IntEnumPropCompanion[EnabledEnumZeroPositive] {
  override val values: Set[EnabledEnumZeroPositive] = Set(`true`, `false`, unknown)
  override val description: String = "Enabled status"
  case object `true` extends EnabledEnumZeroPositive(0, "Enabled")
  case object `false` extends EnabledEnumZeroPositive(1, "Disabled")
  case object unknown extends EnabledEnumZeroPositive(-1, "Unknown")
  val enabled = `true`
  val disabled = `false`

  implicit def boolean2enum(b: Boolean): EnabledEnumZeroPositive = if (b) enabled else disabled
  implicit def boolean2enum(b: Option[Boolean]): Option[EnabledEnumZeroPositive] = b.map(boolean2enum)
}
