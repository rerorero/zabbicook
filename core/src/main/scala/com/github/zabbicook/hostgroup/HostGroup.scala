package com.github.zabbicook.hostgroup

import com.github.zabbicook.entity._
import com.github.zabbicook.hostgroup.HostGroup.HostGroupId
import play.api.libs.json._

sealed abstract class HostGroupFlag(val value: NumProp) extends NumberEnumProp

object HostGroupFlag extends NumberEnumPropCompanion[HostGroupFlag] {
  val all: Set[HostGroupFlag] = Set(plain, discovered, unknown)
  case object plain extends HostGroupFlag(0)
  case object discovered extends HostGroupFlag(4)
  case object unknown extends HostGroupFlag(-1)
}

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/hostgroup/object
  */
case class HostGroup (
  groupid: Option[HostGroupId] = None,  // readonly
  name: String,                         // required
  flags: Option[HostGroupFlag] = None   // readonly
  // internal: Int              // unhandled
) extends Entity {
  def removeReadOnly: HostGroup = {
    copy(groupid = None, flags = None)
  }
}

object HostGroup {
  type HostGroupId = String
  implicit val apiFormat: Format[HostGroup] = Json.format[HostGroup]
}

