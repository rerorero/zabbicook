package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.{Entity, EntityState, _}
import play.api.libs.json.{Format, Json}

case class Application[S <: EntityState](
  applicationid: EntityId = NotStoredId,  // read only
  hostid: EntityId = NotStoredId,         // read only
  name: String
) extends Entity[S] {

  override protected[this] val id: EntityId = applicationid

  def toStored(id: StoredId): Application[Stored] = copy(applicationid = id)

  def setHostId[T >: S <: NotStored](id: StoredId): Application[NotStored] = copy(hostid=id)
}

object Application extends EntityCompanionMetaHelper {

  implicit lazy val format: Format[Application[Stored]] = Json.format[Application[Stored]]

  implicit lazy val format2: Format[Application[NotStored]] = Json.format[Application[NotStored]]

  override val meta = entity("Array of application object.")(
    readOnly("applicationid"),
    readOnly("hostid"),
    value("name")("name")("(required) Name of the application.")
  ) _

  def toNotStored(n: String): Application[NotStored] = Application[NotStored](name = n)
}

