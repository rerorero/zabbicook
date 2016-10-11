package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop._
import play.api.libs.json._

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/hostgroup/object
  */
case class HostGroup[S <: EntityState] (
  groupid: EntityId = NotStoredId,      // readonly
  name: String                         // required
  // flags: Option[HostGroupFlag] = None   // unhandled readonly
  // internal: Int              // unhandled
) extends Entity[S] {

  protected val id: EntityId = groupid

  def toStored(id: StoredId): HostGroup[Stored] = {
    copy(groupid = id).asInstanceOf[HostGroup[Stored]]
  }
}

object HostGroup extends EntityCompanionMetaHelper {
  implicit val apiFormat: Format[HostGroup[NotStored]] = Json.format[HostGroup[NotStored]]

  implicit val apiFormat2: Format[HostGroup[Stored]] = Json.format[HostGroup[Stored]]

  def fromString(name: String): HostGroup[NotStored] = HostGroup(name = name)

  override val meta = entity("Host group object")(
    readOnly("groupid"),
    value("name")("name")("(required) Name of the host group.")
  ) _
}

