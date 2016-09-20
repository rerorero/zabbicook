package com.github.zabbicook.operation

import com.github.zabbicook.entity.Entity

/**
  *
  */
case class Report(
  created: List[Entity] = List(),
  deleted: List[Entity] = List(),
  updated: List[Entity] = List()
) {

  def appendCreated(r: Entity):Report = copy(created = r :: created)

  def appendDeleted(r: Entity):Report = copy(deleted = r :: deleted)

  def appendUpdated(r: Entity):Report = copy(updated = r :: updated)

  def +(r: Report): Report = Report(
    created = created ++ r.created,
    deleted = deleted ++ r.deleted,
    updated = updated ++ r.updated
  )

  def isEmpty(): Boolean = count == 0

  def count: Int = created.length + deleted.length + updated.length
}

object Report {
  def created(e: Entity): Report = Report(created = List(e))
  def created(e: Seq[Entity]): Report = Report(created = e.toList)

  def deleted(e: Entity): Report = Report(deleted = List(e))
  def deleted(e: Seq[Entity]): Report = Report(deleted = e.toList)

  def updated(e: Entity): Report = Report(updated = List(e))
  def updated(e: Seq[Entity]): Report = Report(updated = e.toList)

  def empty(): Report = Report()
}
