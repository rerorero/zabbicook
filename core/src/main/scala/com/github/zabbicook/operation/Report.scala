package com.github.zabbicook.operation

import com.github.zabbicook.entity.Entity
import com.github.zabbicook.entity.Entity.Stored
import play.api.libs.json.{Json, Writes}

/**
  *
  */
case class Report(
  created: List[Entity[Stored]] = List(),
  deleted: List[Entity[Stored]] = List(),
  updated: List[Entity[Stored]] = List()
) {

  def appendCreated(r: Entity[Stored]):Report = copy(created = r :: created)

  def appendDeleted(r: Entity[Stored]):Report = copy(deleted = r :: deleted)

  def appendUpdated(r: Entity[Stored]):Report = copy(updated = r :: updated)

  def +(r: Report): Report = Report(
    created = created ++ r.created,
    deleted = deleted ++ r.deleted,
    updated = updated ++ r.updated
  )

  def isEmpty(): Boolean = count == 0

  def count: Int = created.length + deleted.length + updated.length

  def toStringSeq(): Seq[String] = {
    def toS(count: Int, prefix: String) = if (count > 0) Some(s"$prefix=$count") else None
    (created ++ updated ++ deleted).map(_.entityName).distinct.map { name =>
      val c = toS(created.count(_.entityName == name), "created")
      val u = toS(updated.count(_.entityName == name), "updated")
      val d = toS(deleted.count(_.entityName == name), "deleted")
      s"${name} entities modified(${Seq(c,u,d).flatten.mkString(",")})"
    }
  }
}

object Report {
  def created(e: Entity[Stored]): Report = Report(created = List(e))
  def created(e: Seq[Entity[Stored]]): Report = Report(created = e.toList)

  def deleted(e: Entity[Stored]): Report = Report(deleted = List(e))
  def deleted(e: Seq[Entity[Stored]]): Report = Report(deleted = e.toList)

  def updated(e: Entity[Stored]): Report = Report(updated = List(e))
  def updated(e: Seq[Entity[Stored]]): Report = Report(updated = e.toList)

  def empty(): Report = Report()

  implicit val writesJson: Writes[Report] = Writes[Report] { report =>
    Json.obj(
      "created" -> report.created.length,
      "updated" -> report.updated.length,
      "deleted" -> report.deleted.length,
      "total" -> report.count,
      "messages" -> report.toStringSeq()
    )
  }
}
