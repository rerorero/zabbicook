package com.github.zabbicook.entity

import com.github.zabbicook.entity.prop._
import play.api.libs.json.{Format, Json}

sealed abstract class InventoryMode(val zabbixValue: IntProp, val desc: String) extends EnumProp2[IntProp]

object InventoryMode extends IntEnumProp2Companion[InventoryMode] {
  override val values: Set[InventoryMode] = Set(disabled,manual,automatic,unknown)
  override val description: String = "Host inventory population mode."
  case object disabled extends InventoryMode(-1, "disabled")
  case object manual extends InventoryMode(0, "(default) manual")
  case object automatic extends InventoryMode(1, "automatic")
  case object unknown extends InventoryMode(-999, "unknown")
}

sealed abstract class IpmiAuthAlgo(val zabbixValue: IntProp, val desc: String) extends EnumProp2[IntProp]

object IpmiAuthAlgo extends IntEnumProp2Companion[IpmiAuthAlgo] {
  override val values: Set[IpmiAuthAlgo] = Set(default,none,MD2,MD5,straight,OEM,RMCPPlus,unknown)
  override val description: String = "IPMI authentication algorithm."
  case object default extends IpmiAuthAlgo(-1,"default")
  case object none extends IpmiAuthAlgo(0,"none")
  case object MD2 extends IpmiAuthAlgo(1,"MD2")
  case object MD5 extends IpmiAuthAlgo(2,"MD5")
  case object straight extends IpmiAuthAlgo(4,"straight")
  case object OEM extends IpmiAuthAlgo(5,"OEM")
  case object RMCPPlus extends IpmiAuthAlgo(6,"RMCP+")
  case object unknown extends IpmiAuthAlgo(-999,"unknown")
}

sealed abstract class IpmiPrivilege(val zabbixValue: IntProp, val desc: String) extends EnumProp2[IntProp]

object IpmiPrivilege extends IntEnumProp2Companion[IpmiPrivilege] {
  override val values: Set[IpmiPrivilege] = Set(callback,user,operator,admin,OEM,unknown)
  override val description: String = "IPMI privilege level."
  case object callback extends IpmiPrivilege(1,"callback")
  case object user extends IpmiPrivilege(2,"(default)user")
  case object operator extends IpmiPrivilege(3,"operator")
  case object admin extends IpmiPrivilege(4,"admin")
  case object OEM extends IpmiPrivilege(5,"OEM")
  case object unknown extends IpmiPrivilege(-1,"unknown")
}

case class Host(
  hostid: Option[String] = None,  // readonly
  host: String,                   //(required)	string	Technical name of the host.
  //available	                    // unhandled integer	(readonly) Availability of Zabbix agent.
  description: Option[String] = None,	// optional	Description of the host.
  //disable_until	                // unhandled (readonly) The next polling time of an unavailable Zabbix agent.
  //error	string	                // unhandled (readonly) Error text if Zabbix agent is unavailable.
  //errors_from	                  // unhandled (readonly) Time when Zabbix agent became unavailable.
  //flags	                        // unhandled (readonly) Origin of the host.
  inventory_mode: Option[InventoryMode]=None,	//	Host inventory population mode.
  ipmi_authtype: Option[IpmiAuthAlgo]=None,	//	IPMI authentication algorithm.
  //ipmi_available	              // unhandled	(readonly) Availability of IPMI agent.
  //ipmi_disable_until	          // unhandled	(readonly) The next polling time of an unavailable IPMI agent.
  //ipmi_error	                  // unhandled	(readonly) Error text if IPMI agent is unavailable.
  //ipmi_errors_from	            // unhandled 	(readonly) Time when IPMI agent became unavailable.
  ipmi_password: Option[String]=None,  // IPMI password.
  ipmi_privilege: Option[IpmiPrivilege]=None, //	IPMI privilege level.
  ipmi_username: Option[String]=None, 	// IPMI username.
  //jmx_available	                // unhandled 	(readonly) Availability of JMX agent.
  //jmx_disable_until	            // unhandled	(readonly) The next polling time of an unavailable JMX agent.
  //jmx_error	string	            // unhandled (readonly) Error text if JMX agent is unavailable.
  //jmx_errors_from	              // unhandled	(readonly) Time when JMX agent became unavailable.
  //maintenance_from	            // unhandled 	(readonly) Starting time of the effective maintenance.
  //maintenance_status	          // unhandled	(readonly) Effective maintenance status.
  //maintenance_type	            // unhandled 	(readonly) Effective maintenance type.
  //maintenanceid	                // unhandled	(readonly) ID of the maintenance that is currently in effect on the host.
  name: Option[String] = None,	  // Visible name of the host.
  // proxy_hostid: Option[HostId]=None,	//	unhandled. Not supported yet. CID of the proxy that is used to monitor the host.
  //snmp_available	              // unhandled 	(readonly) Availability of SNMP agent.
  //snmp_disable_until	          // unhandled 	(readonly) The next polling time of an unavailable SNMP agent.
  //snmp_error	                  // unhandled 	(readonly) Error text if SNMP agent is unavailable.
  //snmp_errors_from	            // unhandled 	(readonly) Time when SNMP agent became unavailable.
  status: Option[EnabledEnumZeroPositive]=None	//	Status and function of the host.(0 - (default) monitored host; 1 - unmonitored host.)
)

object Host extends EntityCompanionMetaHelper {
  import Meta._

  implicit val format: Format[Host] = Json.format[Host]

  val meta = entity("The host object")(
    readOnly("hostid"),
    value("host")("hostname", "host")("(required) Technical name of the host."),
    value("description")("description", "desc")("Description of the host."),
    InventoryMode.meta("inventory_mode")("inventoryMode"),
    IpmiAuthAlgo.meta("ipmi_authtype")("ipmiAuthAlgorithm"),
    value("ipmi_password")("ipmiPass")("IPMI password."),
    IpmiPrivilege.meta("ipmi_privilege")("ipmiPrivilegeLevel"),
    value("ipmi_username")("ipmiUser")("IPMI username."),
    value("name")("visibleName")(
      """Visible name of the host.
        ||Default: host property value."""),
    enum("status", EnabledEnumZeroPositive.values)("status")("Status and function of the host.")
  ) _
}
