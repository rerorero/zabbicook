package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.{EnabledEnum, IntProp}
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

case class Action[S <: EntityState](
  actionid: EntityId = NotStoredId,
  esc_period: IntProp,
  eventsource: EventSource,
  name: String,
  def_longdata: Option[String],
  def_shortdata: Option[String],
  r_longdata: Option[String],
  r_shortdata: Option[String],
  recovery_msg: Option[EnabledEnum], // ver <= 3.0.x
  status: Option[EnabledEnum]
) extends Entity[S] {
  override protected[this] def id: EntityId = actionid
  def toStored(id: StoredId): Action[Stored] = copy(actionid = id)
}

object Action {
  implicit val format: Format[Action[Stored]] = Json.format[Action[Stored]]
  implicit val format2: Format[Action[NotStored]] = Json.format[Action[NotStored]]
}
