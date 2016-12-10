package com.github.zabbicook.doc

import java.io.PrintWriter

import com.github.zabbicook.entity.prop.{ArrayMeta, EntityMeta, Meta}
import com.github.zabbicook.recipe.Recipe

case class HtmlMeta(
  aliases: Set[String],
  path: String,
  contentId: String,
  menuId: String,
  parentId: String,
  description: Seq[String],
  possibles: Option[Seq[String]],
  children: Seq[HtmlMeta],
  offset: Int
) {
  def fullName(): String = {
    val (head, tail) = aliases.splitAt(1)
    val opt = if (tail.isEmpty) "" else s" (${tail.mkString(", ")})"
    if (path.isEmpty) s"${head.head}${opt}" else s"${path}.${head.head}${opt}"
  }

  def name(): String = aliases.headOption.getOrElse("---")

  def heading(): Int = {
    if (children.isEmpty)
      4
    else if (path.isEmpty)
      1
    else
      2
  }
}

object HtmlMeta {
  private def flattenRoute(route: Seq[String], separator: String): String = {
    route.fold("") {
      case (acc, "") => acc
      case (acc, s) => (acc + separator + s)
    } drop 1
  }

  def fromMeta(meta: Meta, baseRoute: Seq[String] = Seq()): HtmlMeta = {
    val route: Seq[String] = baseRoute ++ Seq(meta.aliases.headOption).flatten
    val description = {
      val desc = meta.desc.desc.split(System.lineSeparator()).toList
      if (meta.isInstanceOf[ArrayMeta]) "(Array object)" :: desc else desc
    }
    val offset = if (baseRoute.isEmpty || baseRoute.length > 3) 0 else 1
    val children = meta match {
      case m: EntityMeta =>
        m.entity.filter(!_.readOnly).map(fromMeta(_, route))
      case ArrayMeta(_,_,_, Some(m: EntityMeta)) =>
        m.entity.filter(!_.readOnly).map(fromMeta(_, route))
      case _ =>
        Seq()
    }
    HtmlMeta(
      aliases = meta.aliases,
      path = flattenRoute(baseRoute, "."),
      contentId = "m-" + flattenRoute(route, "-"),
      menuId = "c-" + flattenRoute(route, "-"),
      parentId = "c-" + flattenRoute(baseRoute, "-"),
      description = description,
      possibles = meta.desc.possibles,
      children = children,
      offset = offset
    )
  }
}

object HtmlDoc {
  def main(args: Array[String]): Unit = {
    val writer = new PrintWriter("src/site/index.html")
    val tree = HtmlMeta.fromMeta(Recipe.required(""))
    writer.write(html.index(tree.children).toString())
    writer.close
  }

}
