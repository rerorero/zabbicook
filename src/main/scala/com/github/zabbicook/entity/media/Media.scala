package com.github.zabbicook.entity.media

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.{EnabledEnumZeroPositive, IntProp}
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

case class Media[S <: EntityState](
  mediaid: EntityId = NotStoredId,
  active: EnabledEnumZeroPositive,
  mediatypeid: EntityId = NotStoredId,
  period: String,
  sendto: String,
  severity: IntProp
) extends Entity[S] {
  override protected[this] def id: EntityId = mediaid

  def toStored(id: StoredId): Media[Stored] = copy(mediaid = id)

  def isSame[T <: EntityState](that: Media[T]): Boolean = {
    (active == that.active) &&
      (mediatypeid == that.mediatypeid) &&
      (period == that.period) &&
      (sendto == that.sendto) &&
      (severity == that.severity)
  }
}

object Media {
  implicit val format: Format[Media[NotStored]] = Json.format[Media[NotStored]]

  implicit val format2: Format[Media[Stored]] = Json.format[Media[Stored]]
}
