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

sealed trait EntityId extends Ordered[EntityId] {
  def toStored: EntityId.StoredId
  def toNotStored: EntityId.NotStoredId
}

object EntityId {
  case class StoredId(id: String) extends EntityId {
    def toStored: StoredId = this
    def toNotStored: NotStoredId = NotStoredId

    override def compare(that: EntityId): Int = {
      that match {
        case StoredId(rid) => id.compare(rid)
        case _: NotStoredId => 1
      }
    }
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

    override def compare(that: EntityId): Int = that match {
      case _: StoredId => -1
      case _: NotStoredId => 0
    }
  }

  case object NotStoredId extends NotStoredId

  implicit val format: Format[EntityId] = Format(
    Reads[EntityId] {
      case JsNumber(n) => JsSuccess(StoredId(n.toString))
      case JsString(s) => JsSuccess(StoredId(s))
      case els => JsError(s"Invalid entity id format: ${els}")
    },
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

  implicit val readsStored: Reads[StoredId] =
    Reads[StoredId] {
      case JsNumber(n) => JsSuccess(StoredId(n.toString))
      case JsString(s) => JsSuccess(StoredId(s))
      case els => JsError(s"Invalid entity id format: ${els}")
    }
}

trait PropCompare {
  protected[this] def isSameProp[A](self: Option[A], constant: Option[A], defValue: A, compare: (A, A) => Boolean = (a:A, b:A) => a == b): Boolean = {
    (self, constant) match {
      case (Some(s), Some(c)) if compare(s, c) => true
      case (Some(s), None) if compare(s, defValue) => true
      case (None, None) => true
      case _ => false
    }
  }
}

abstract class Entity[S <: EntityState] extends PropCompare { self =>
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
