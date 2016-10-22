package com.github.zabbicook.entity.media

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.prop.{EnabledEnumZeroPositive, IntProp}
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

case class Media[S <: EntityState](
  mediaid: EntityId,
  active: EnabledEnumZeroPositive,
  mediatypeid: EntityId,
  period: String,
  sendto: String,
  severity: IntProp
) extends Entity[S] {
  override protected[this] def id: EntityId = mediaid
}

object Media {
  implicit val format: Format[Media[NotStored]] = Json.format[Media[NotStored]]

  implicit val format2: Format[Media[Stored]] = Json.format[Media[Stored]]
}

