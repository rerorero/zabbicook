package com.github.zabbicook.entity.item

import com.github.zabbicook.entity._
import com.github.zabbicook.entity.prop.{IntEnumDescribedWithString, IntEnumDescribedWithStringCompanion, IntProp}

sealed abstract class ItemType(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = ItemType.validate(this)
}

object ItemType extends IntEnumDescribedWithStringCompanion[ItemType] {
  override val all: Set[ItemType] = Set(
    ZabbixAgent,
    SNMPv1agent,
    ZabbixTrapper ,
    SimpleCheck ,
    SNMPv2Agent ,
    ZabbixInternal ,
    SNMPv3Agent ,
    ZabbixAgentActive ,
    ZabbixAggregate ,
    WebItem ,
    ExternalCheck ,
    DatabaseMonitor ,
    IPMIagent ,
    SSHagent ,
    TELNETagent ,
    calculated ,
    JMXagent ,
    SNMPtrap ,
    unknown
  )
  case object ZabbixAgent extends ItemType(0)
  case object SNMPv1agent extends ItemType(1)
  case object ZabbixTrapper extends ItemType(2)
  case object SimpleCheck extends ItemType(3)
  case object SNMPv2Agent extends ItemType(4)
  case object ZabbixInternal extends ItemType(5)
  case object SNMPv3Agent extends ItemType(6)
  case object ZabbixAgentActive extends ItemType(7)
  case object ZabbixAggregate extends ItemType(8)
  case object WebItem extends ItemType(9)
  case object ExternalCheck extends ItemType(10)
  case object DatabaseMonitor extends ItemType(11)
  case object IPMIagent extends ItemType(12)
  case object SSHagent extends ItemType(13)
  case object TELNETagent extends ItemType(14)
  case object calculated extends ItemType(15)
  case object JMXagent extends ItemType(16)
  case object SNMPtrap extends ItemType(17)
  case object unknown extends ItemType(-1)
}

