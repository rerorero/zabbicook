package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.prop._

sealed abstract class ItemType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object ItemType extends IntEnumPropCompanion[ItemType] {
  override val values: Seq[ItemType] = Seq(
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
  override val description = "(required) Type of item."
  case object ZabbixAgent extends ItemType(0, "Zabbix agent")
  case object SNMPv1agent extends ItemType(1, "SNMPv1 agent")
  case object ZabbixTrapper extends ItemType(2, "Zabbix trapper")
  case object SimpleCheck extends ItemType(3, "Simple check")
  case object SNMPv2Agent extends ItemType(4, "SNMPv2 agent")
  case object ZabbixInternal extends ItemType(5, "Zabbix internal")
  case object SNMPv3Agent extends ItemType(6, "SNMPv3 agent")
  case object ZabbixAgentActive extends ItemType(7, "Zabbix agent (active)")
  case object ZabbixAggregate extends ItemType(8, "Zabbix aggregate")
  case object WebItem extends ItemType(9, "web item")
  case object ExternalCheck extends ItemType(10, "external check")
  case object DatabaseMonitor extends ItemType(11, "database monitor")
  case object IPMIagent extends ItemType(12, "IPMI agent")
  case object SSHagent extends ItemType(13, "SSH agent")
  case object TELNETagent extends ItemType(14, "TELNET agent")
  case object calculated extends ItemType(15, "calculated")
  case object JMXagent extends ItemType(16, "JMX agent")
  case object SNMPtrap extends ItemType(17, "SNMP trap")
  case object unknown extends ItemType(-1, "unknown")
}

