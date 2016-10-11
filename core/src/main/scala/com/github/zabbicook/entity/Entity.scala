package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.Stored
import com.github.zabbicook.entity.EntityId.StoredId
import play.api.libs.json._

/**
  * EntityState is a phantom type which describes entity stored state
  * Stored - Entity has a database-unique entity id.
  * NotStored - Entity has no ids.
  */
sealed trait EntityState
object Entity {
  final class Stored extends EntityState
  final class NotStored extends EntityState
}

sealed trait EntityId {
  def toStored: EntityId.StoredId
  def toNotStored: EntityId.NotStoredId
}

object EntityId {
  case class StoredId(id: String) extends EntityId {
    def toStored: StoredId = this
    def toNotStored: NotStoredId = NotStoredId
  }

  class NotStoredId extends EntityId {
    def toStored: StoredId = {
      // Not type safed ...
      sys.error(s"Unexpected operation. Entity has no id.")
    }
    def toNotStored: NotStoredId = this
    override def equals(that: Any): Boolean =
      that match {
        case that: NotStoredId => true
        case _ => false
      }
  }

  case object NotStoredId extends NotStoredId

  implicit val format: Format[EntityId] = Format(
    Reads.StringReads.map(StoredId),
    Writes[EntityId] {
      case _: NotStoredId => JsNull
      case StoredId(v) => JsString(v)
    }
  )

  def isDifferent(a: EntityId, b: EntityId): Boolean = {
    (a, b) match {
      case (StoredId(aid), StoredId(bid)) => aid != bid
      case _ => false
    }
  }

  implicit val readsStored: Reads[StoredId] = Reads.StringReads.map(StoredId)
}

abstract class Entity[S <: EntityState] { self =>
  protected[this] def id: EntityId

  def getStoredId[T >: S <: Stored]: StoredId = id.toStored

  def entityName: String = getClass.getSimpleName

  protected[this] def shouldBeUpdated[T](src: Option[T], dest: Option[T]): Boolean = {
    (src, dest) match {
      case (Some(s), Some(d)) if s != null && d != null => s != d
      case _ => false
    }
  }
}
