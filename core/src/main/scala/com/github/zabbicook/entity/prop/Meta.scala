package com.github.zabbicook.entity.prop

case class Description(
  desc: String,
  possibles: Option[Set[String]] = None
)

sealed trait Meta{
  def name: String
  def aliases: Set[String]
  def desc: Description

  def formatAliases: String = aliases.map(s => s"[$s]").mkString("")
  def description: String = {
    val possibles = desc.possibles.map( p =>
      if (p.isEmpty) "" else "\t" + p.mkString(System.lineSeparator() + "\t")
    ).getOrElse("")
    s"""$formatAliases
       |\t${desc.desc}
       |${possibles}
     """.stripMargin
  }
}

case class PropMeta(
  name: String,
  aliases: Set[String],
  desc: Description
) extends Meta

case class EnumMeta(
  name: String,
  aliases: Set[String],
  desc: Description,
  values: Set[String]
) extends Meta

case class ArrayMeta(
  name: String,
  aliases: Set[String],
  desc: Description,
  elements: Option[Meta]
) extends Meta

case class EntityMeta(
  name: String,
  aliases: Set[String],
  desc: Description,
  entity: Seq[Meta],
  required: Boolean
) extends Meta {
  val alias = name
  def findByName(name: String): Option[Meta] = entity.find(_.name == name)
  val entityAliases: Set[String] = entity.map(_.aliases).flatten.toSet
}

object Meta {

  def value(name: String)(aliases: String*)(desc: String): PropMeta =
    PropMeta(name, aliases.toSet, Description(desc))

  def readOnly(name: String): PropMeta =
    PropMeta(name, Set(), Description("This is read only property."))

  def enum[T <: EnumProp2[_]](name: String, values: Set[T])(aliases: String*)(desc: String): EnumMeta = {
    val possibles = values.map(v => s"'${v.toString}' - ${v.desc}")
    EnumMeta(name, aliases.toSet, Description(desc, Some(possibles)), values.map(_.toString))
  }

  def array(name: String)(aliases: String*)(desc: String): ArrayMeta =
    ArrayMeta(name, aliases.toSet, Description(desc), None)

  def arrayOf(name: String)(meta: Meta): ArrayMeta =
    ArrayMeta(name, meta.aliases, Description(s"Array of entities: ${meta.aliases.headOption.getOrElse("")}"), Some(meta))

  def entity(desc: String)(elements: Meta*)(name: String, required: Boolean): EntityMeta =
    new EntityMeta(name, Set(name), Description(desc), elements, required)
}

trait EntityCompanionMetaHelper {
  def meta: (String, Boolean) => EntityMeta
  def required(name: String) = meta(name, true)
  def optional(name: String) = meta(name, false)
}
