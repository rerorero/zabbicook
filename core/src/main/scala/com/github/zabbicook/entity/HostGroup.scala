package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.{IntProp, IntEnumDescribedWithString, IntEnumDescribedWithStringCompanion}
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json._

sealed abstract class HostGroupFlag(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = HostGroupFlag.validate(this)
}

object HostGroupFlag extends IntEnumDescribedWithStringCompanion[HostGroupFlag] {
  val all: Set[HostGroupFlag] = Set(plain, discovered, unknown)
  case object plain extends HostGroupFlag(0)
  case object discovered extends HostGroupFlag(4)
  case object unknown extends HostGroupFlag(-1)
}

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/hostgroup/object
  */
case class HostGroup[S <: EntityState] (
  groupid: EntityId = NotStoredId,      // readonly
  name: String,                         // required
  flags: Option[HostGroupFlag] = None   // readonly
  // internal: Int              // unhandled
) extends Entity[S] {

  protected val id: EntityId = groupid

  def toStored(id: StoredId): HostGroup[Stored] = {
    copy(groupid = id).asInstanceOf[HostGroup[Stored]]
  }
}

object HostGroup {
  implicit val apiFormat: Format[HostGroup[NotStored]] = Json.format[HostGroup[NotStored]]
  implicit val apiFormat2: Format[HostGroup[Stored]] = Json.format[HostGroup[Stored]]

  implicit val hoconReads: HoconReads[HostGroup[NotStored]] = {
    required[String]("name").map(fromString)
  }

  def fromString(name: String): HostGroup[NotStored] = HostGroup(name = name)
}

