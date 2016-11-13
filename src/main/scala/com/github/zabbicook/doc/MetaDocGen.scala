package com.github.zabbicook.doc

import com.github.zabbicook.entity.prop._

import scala.annotation.tailrec
import scala.util.Try

case class MetaDocGenError(msg: String) extends Exception(msg)

object MetaDocGen {

  /**
    * Returns StringBuilder of meta information by specified path.
    */
  def pathOf(path: String, meta: Meta, maxDepth: Int = Int.MaxValue): Try[StringBuilder] = {
    val paths = path.split("\\.")
    (meta, paths.headOption, paths.tail) match {
      case (e, Some(""), _) =>
        Try(genBuilder(e, maxDepth = maxDepth))

      case (e: EntityMeta, Some(prop), remains) =>
        e.entity.find(_.aliases.contains(prop)) match {
          case Some(propMeta) if remains.isEmpty  =>
            Try(genBuilder(propMeta, maxDepth = maxDepth))
          case Some(propMeta) if remains.nonEmpty =>
            pathOf(remains.mkString("."), propMeta, maxDepth)
          case None =>
            throw MetaDocGenError(
              s"""Invalid path, '$prop' no such property.
                 |Available properties: ${e.entity.map(_.aliases).flatten.mkString(", ")}""".stripMargin)
        }

      case (e: ArrayMeta, Some(prop), remains) =>
        e.element match {
          case Some(elem) =>
            pathOf(path, elem, maxDepth)
          case None if remains.isEmpty =>
            Try(genBuilder(e, maxDepth = maxDepth))
          case None if remains.nonEmpty =>
            throw MetaDocGenError(s"Invalid path, '${remains.head}' no such property.")
        }

      case (e, Some(prop), _)  =>
        throw MetaDocGenError(s"Invalid path, '$prop' no such property.")

      case (e, None, _) =>
        Try(genBuilder(e, maxDepth = maxDepth))
    }
  }

  /**
    * Returns StringBuilder that shows tree of meta information
    */
  def genBuilder(
    meta: Meta,
    sb: StringBuilder = new StringBuilder(),
    depth: Int = 0,
    maxDepth: Int = Int.MaxValue
  ): StringBuilder = {
    if (!meta.readOnly && depth <= maxDepth) {
      val metasb = new StringBuilder()
      appendAlias(metasb, meta)
      metasb.append(System.lineSeparator())

      val desc = new StringBuilder()
      desc.append(meta.desc.desc)
      meta.desc.possibles.foreach { possibles =>
        possibles.foreach(p => desc ++= System.lineSeparator() + p)
      }
      desc.append(System.lineSeparator())

      def appendProperty(entity: EntityMeta): StringBuilder = {
        indent(desc, "│","│")
        metasb.append(desc)
        def appendChild(child: Meta, tab: String, head: String) = {
          val childsb = genBuilder(child, depth = depth + 1, maxDepth = maxDepth)
          indent(childsb, tab, head)
          metasb.append(System.lineSeparator())
          metasb.append(childsb)
        }
        val entities = entity.entity.filter(!_.readOnly)
        val (forward, last) = entities.splitAt(entities.length - 1)
        forward.foreach(appendChild(_, "│  ","├─ "))
        last.foreach(appendChild(_, "   ","└─ "))
        metasb
      }

      meta match {
        case ArrayMeta(_,_,_, Some(entity: EntityMeta)) if depth != maxDepth=>
          appendProperty(entity)
        case entity: EntityMeta if depth != maxDepth =>
          appendProperty(entity)
        case _ =>
          metasb.append(desc)
      }
      sb.append(metasb)
    }
    sb
  }

  private[this] def appendAlias(sb: StringBuilder, meta: Meta): Unit = {
    def typ(m: Meta): Option[String] = m match {
      case ArrayMeta(_,_,_, Some(entity: EntityMeta)) =>
        typ(entity).map(e => s"array of ${e}s").orElse(Some("array"))
      case _: EntityMeta =>
        Some("object")
      case _: EnumMeta =>
        Some("enum string")
      case _ =>
        None
    }
    if (meta.aliases.forall(_.isEmpty)) {
      sb ++= "(root)"
    } else {
      sb ++= "[ " + meta.aliases.mkString(", ") + " ]"
      typ(meta).foreach { t =>
        sb ++= " (" + t + ")"
      }
    }
  }

  private[this] def indent(sb: StringBuilder, tab: String, headTab: String): Unit = {
    @tailrec
    def insertAll(sb: StringBuilder, mark: String, word: String, baseIndex: Int = 0): Unit = {
      val index = sb.indexOf(mark, baseIndex)
      if (index != -1) {
        sb.insert(index + mark.length, word)
        insertAll(sb, mark, word, index + word.length + 1)
      }
    }
    sb.insert(0, headTab)
    insertAll(sb, System.lineSeparator(), tab)
  }
}
