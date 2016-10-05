package com.github.zabbicook.entity

import com.github.zabbicook.entity.Host.HostEnabled
import com.github.zabbicook.entity.prop.{EnabledEnumZeroPositive, IntProp, IntEnumDescribedWithString, IntEnumDescribedWithStringCompanion}
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{Format, Json}

sealed abstract class InventoryMode(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = InventoryMode.validate(this)
}

object InventoryMode extends IntEnumDescribedWithStringCompanion[InventoryMode] {
  override val all: Set[InventoryMode] = Set(disabled,manual,automatic,unknown)
  case object disabled extends InventoryMode(-1)
  case object manual extends InventoryMode(0)
  case object automatic extends InventoryMode(1)
  case object unknown extends InventoryMode(-999)
}

sealed abstract class IpmiAuthAlgo(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = IpmiAuthAlgo.validate(this)
}

object IpmiAuthAlgo extends IntEnumDescribedWithStringCompanion[IpmiAuthAlgo] {
  override val all: Set[IpmiAuthAlgo] = Set(default,none,MD2,MD5,straight,OEM,RMCPPlus,unknown)
  case object default extends IpmiAuthAlgo(-1)
  case object none extends IpmiAuthAlgo(0)
  case object MD2 extends IpmiAuthAlgo(1)
  case object MD5 extends IpmiAuthAlgo(2)
  case object straight extends IpmiAuthAlgo(4)
  case object OEM extends IpmiAuthAlgo(5)
  case object RMCPPlus extends IpmiAuthAlgo(6)
  case object unknown extends IpmiAuthAlgo(-999)
}

sealed abstract class IpmiPrivilege(val value: IntProp) extends IntEnumDescribedWithString {
  override def validate(): ValidationResult = IpmiPrivilege.validate(this)
}

object IpmiPrivilege extends IntEnumDescribedWithStringCompanion[IpmiPrivilege] {
  override val all: Set[IpmiPrivilege] = Set(callback,user,operator,admin,OEM,unknown)
  case object callback extends IpmiPrivilege(1)
  case object user extends IpmiPrivilege(2)
  case object operator extends IpmiPrivilege(3)
  case object admin extends IpmiPrivilege(4)
  case object OEM extends IpmiPrivilege(5)
  case object unknown extends IpmiPrivilege(-1)
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
  status: Option[HostEnabled]=None	//	Status and function of the host.(0 - (default) monitored host; 1 - unmonitored host.)
)

object Host {
  type HostEnabled = EnabledEnumZeroPositive

  implicit val format: Format[Host] = Json.format[Host]

  implicit val hocon: HoconReads[Host] = {
    val reads = for {
      host <- required[String]("hostname")
      description <- optional[String]("description")
      inventory_mode <- optional[InventoryMode]("inventoryMode")
      ipmi_authtype <- optional[IpmiAuthAlgo]("ipmiAuthAlgorithm")
      ipmi_privilege <- optional[IpmiPrivilege]("ipmiPrivilegeLevel")
      ipmi_username <- optional[String]("ipmiUser")
      ipmi_password <- optional[String]("ipmiPass")
      name <- optional[String]("visibleName")
      status <- optional[HostEnabled]("enabled")
    } yield {
      Host(
        host = host,
        description = description,
        inventory_mode = inventory_mode,
        ipmi_authtype = ipmi_authtype,
        ipmi_privilege = ipmi_privilege,
        ipmi_username = ipmi_username,
        ipmi_password = ipmi_password,
        name = name,
        status = status
      )
    }
    reads.withAcceptableKeys(
      "hostname",
      "description",
      "inventoryMode",
      "ipmiAuthAlgorithm",
      "ipmiPrivilegeLevel",
      "ipmiUser",
      "ipmiPass",
      "visibleName",
      "enabled"
    )
  }
}
