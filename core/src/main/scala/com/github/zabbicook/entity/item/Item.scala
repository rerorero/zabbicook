package com.github.zabbicook.entity.item

import ai.x.play.json.Jsonx
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity._
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop._
import play.api.libs.json.{Format, JsObject, Json}

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/item/object
  */
case class Item[S <: EntityState](
  itemid: EntityId = NotStoredId, // read only
  delay: IntProp,                 // required Update interval of the item in seconds.
  hostid: EntityId = NotStoredId, // required
  //interfaceid: EntityId = NotStoredId, // required, unhandled TODO ID of the item's host interface. Used only for host items.
  `key_`: String,                   // required
  name: String,                   // required
  `type`: ItemType,               // required Type of the item
  value_type: ValueType,          // required Type of information of the item.
  authtype: Option[AuthType] = None,  //integer	SSH authentication method. Used only by SSH agent items.
  data_type: Option[DataType]=None,   // Data type of the item
  //delay_flex:                       // unhandled	TODO string	Custom intervals that contain flexible intervals and scheduling intervals as serialized strings.
                                      // Multiple intervals are separated by a semicolon.
  delta: Option[Delta] = None,	      // Value that will be stored.
  description: Option[String] = None, // Description of the item.
  //error	                            // unhandled string	(readonly) Error text if there are problems updating the item.
  //flags	                            // unhandled integer	(readonly) Origin of the item.
  formula: Option[DoubleProp] = None, // integer/float	Custom multiplier.
  history: Option[IntProp] = None,	  // integer	Number of days to keep item's history data.
  inventory_link: Option[IntProp] = None, //	integer	ID of the host inventory field that is populated by the item.
                                      // @see https://www.zabbix.com/documentation/3.2/manual/api/reference/host/object#host_inventory
  ipmi_sensor: Option[String]=None,   // string	IPMI sensor. Used only by IPMI items.
  //lastclock	                        // unhandled timestamp	(readonly) Time when the item was last updated.
  //lastns	                          // unhandled integer	(readonly) Nanoseconds when the item was last updated.
  //lastvalue                         // unhandled	(readonly) Last value of the item.
  logtimefmt: Option[String]=None,    // string	Format of the time in log entries. Used only by log items.
  //mtime	                            // unhandled timestamp	Time when the monitored log file was last updated. Used only by log items.
  multiplier: Option[EnabledEnum]=None,// integer	Whether to use a custom multiplier.
  params: Option[String]=None,        // string	Additional parameters depending on the type of the item:
  password: Option[String]=None,	    // string	Password for authentication. Used by simple check, SSH, Telnet, database monitor and JMX items.
  port: Option[IntProp]=None,         // !!nullable (it has no default values) // string	Port monitored by the item. Used only by SNMP items.
  //prevvalue: option[String]=None,   // unhandled string	(readonly) Previous value of the item.
  privatekey: Option[String]=None,    //	string	Name of the private key file.
  publickey: Option[String]=None,	    // string	Name of the public key file.
  snmp_community: Option[String]=None,//	string	SNMP community. Used only by SNMPv1 and SNMPv2 items.
  snmp_oid: Option[String]=None,      // 	string	SNMP OID.
  snmpv3_authpassphrase: Option[String] =None,  //	string	SNMPv3 auth passphrase. Used only by SNMPv3 items.
  snmpv3_authprotocol: Option[SNMPV3AuthProtocol]=None,        // integer	SNMPv3 authentication protocol. Used only by SNMPv3 items.
  snmpv3_contextname: Option[String] =None,     //	string	SNMPv3 context name. Used only by SNMPv3 items.
  snmpv3_privpassphrase: Option[String]=None,   //	string	SNMPv3 priv passphrase. Used only by SNMPv3 items.
  snmpv3_privprotocol: Option[SNMPV3PrivProtocol]=None,	      // integer	SNMPv3 privacy protocol. Used only by SNMPv3 items.
  snmpv3_securitylevel: Option[SNMPV3SecurityLevel]=None,     //	integer	SNMPv3 security level. Used only by SNMPv3 items.
  snmpv3_securityname: Option[String]=None,     //	string	SNMPv3 security name. Used only by SNMPv3 items.
  //state	                            // unhandled integer	(readonly) State of the item.
  status: Option[EnabledEnumZeroPositive]=None,   //	integer	Status of the item.
  //templateid	                      // unhandled string	(readonly) ID of the parent template item.
  trapper_hosts: Option[String]=None, //	string	Allowed hosts. Used only by trapper items.
  trends: Option[IntProp]=None,           // 	integer	Number of days to keep item's trends data.
  units: Option[String]=None,         // 	string	Value units.
  username: Option[String]=None       //	string	Username for authentication. Used by simple check, SSH, Telnet, database monitor and JMX items.
  //valuemapid: EntityId	            // unhandled TODO string	ID of the associated value map.
) extends Entity[S] {

  override protected[this] val id: EntityId = itemid

  def toStored(id: StoredId): Item[Stored] = copy(itemid = id)

  def setHost[T >: S <: NotStored](_hostid: StoredId): Item[NotStored] = copy(hostid = _hostid)

  def toJsonForUpdate[T >: S <: NotStored](_id: StoredId): JsObject = {
    Json.toJson(copy(itemid = _id).asInstanceOf[Item[Stored]]).as[JsObject]
  }

  /**
    * compare object to check whether or not you need to update the stored object
    * @param constant with which to compare(requires same name)
    * @return true: need to update stored entity
    *         false: There is no differences.
    */
  def shouldBeUpdated[T >: S <: Stored](constant: Item[NotStored]): Boolean = {
    require(`key_` == constant.`key_`)

    delay != constant.delay ||
    EntityId.isDifferent(hostid, constant.hostid) ||
    name != constant.name ||
    `type` != constant.`type` ||
    value_type != constant.value_type ||
    shouldBeUpdated(authtype, constant.authtype) ||
    shouldBeUpdated(delta, constant.delta) ||
    shouldBeUpdated(description, constant.description) ||
    shouldBeUpdated(formula, constant.formula) ||
    shouldBeUpdated(history, constant.history) ||
    shouldBeUpdated(inventory_link, constant.inventory_link) ||
    shouldBeUpdated(ipmi_sensor, constant.ipmi_sensor) ||
    shouldBeUpdated(logtimefmt, constant.logtimefmt) ||
    shouldBeUpdated(multiplier, constant.multiplier) ||
    shouldBeUpdated(params, constant.params) ||
    shouldBeUpdated(password, constant.password) ||
    shouldBeUpdated(port, constant.port) ||
    shouldBeUpdated(privatekey, constant.privatekey) ||
    shouldBeUpdated(publickey, constant.publickey) ||
    shouldBeUpdated(snmp_community, constant.snmp_community) ||
    shouldBeUpdated(snmp_oid, constant.snmp_oid) ||
    shouldBeUpdated(snmpv3_authpassphrase, constant.snmpv3_authpassphrase) ||
    shouldBeUpdated(snmpv3_authprotocol, constant.snmpv3_authprotocol) ||
    shouldBeUpdated(snmpv3_contextname, constant.snmpv3_contextname) ||
    shouldBeUpdated(snmpv3_privpassphrase, constant.snmpv3_privpassphrase) ||
    shouldBeUpdated(snmpv3_privprotocol, constant.snmpv3_privprotocol) ||
    shouldBeUpdated(snmpv3_securitylevel, constant.snmpv3_securitylevel) ||
    shouldBeUpdated(snmpv3_securityname, constant.snmpv3_securityname) ||
    shouldBeUpdated(status, constant.status) ||
    shouldBeUpdated(trapper_hosts, constant.trapper_hosts) ||
    shouldBeUpdated(trends, constant.trends) ||
    shouldBeUpdated(units, constant.units) ||
    shouldBeUpdated(username, constant.username)
  }
}

object Item extends EntityCompanionMetaHelper {
  // @see https://github.com/playframework/playframework/issues/3174
  implicit lazy val format: Format[Item[Stored]] = Jsonx.formatCaseClass[Item[Stored]]

  implicit lazy val format2: Format[Item[NotStored]] = Jsonx.formatCaseClass[Item[NotStored]]

  override val meta = entity("Item object")(
    readOnly("itemid"),
    value("delay")("interval","delay")("(required) Update interval of the item in seconds."),
    readOnly("hostid"), // provided by the template settings
    value("key_")("key")("(required) Item key."),
    value("name")("name")("(required) Name of the item."),
    ItemType.meta("type")("type"),
    ValueType.meta("value_type")("typeOfInformation","valueType"),
    AuthType.meta("authtype")("authMethod","authType"),
    DataType.meta("data_type")("dataType"),
    Delta.meta("delta")("delta"),
    value("description")("description", "desc")("Description of the item."),
    value("formula")("multiplier", "formula")("""(integer/float) Custom multiplier.
                                                |Default: 1.""".stripMargin),
    value("history")("history")("""Number of days to keep item's history data.
                                  |Default: 90. """.stripMargin),
    value("inventory_link")("hostInventoryField")(
      """ID of the host inventory field that is populated by the item.
        |Refer to the host inventory page for a list of supported host inventory fields and their IDs.
        |Default: 0. """.stripMargin),
    value("ipmi_sensor")("IPMISensor")("IPMI sensor. Used only by IPMI items."),
    value("logtimefmt")("logTimeFormat")("Format of the time in log entries. Used only by log items."),
    EnabledEnum.metaWithDesc("multiplier")("multiplierEnabled")("Whether to use a custom multiplier."),
    value("params")("params")("""Additional parameters depending on the type of the item:
                                |- executed script for SSH and Telnet items;
                                |- SQL query for database monitor items;
                                |- formula for calculated items.""".stripMargin),
    value("password")("password")("Password for authentication. Used by simple check, SSH, Telnet, database monitor and JMX items."),
    value("port")("port")("Port monitored by the item. Used only by SNMP items."),
    value("privatekey")("privateKey")("Name of the private key file."),
    value("publickey")("publicKey")("Name of the public key file."),
    value("snmp_community")("SNMPCommunity")("SNMP community. Used only by SNMPv1 and SNMPv2 items."),
    value("snmp_oid")("SNMPOID", "OID")("SNMP OID."),
    value("snmpv3_authpassphrase")("SNMPAuthPassphrase")("SNMPv3 auth passphrase. Used only by SNMPv3 items."),
    SNMPV3AuthProtocol.meta("snmpv3_authprotocol")("SNMPAuthProtocol"),
    value("snmpv3_contextname")("SNMPContextName")("SNMPv3 context name. Used only by SNMPv3 items."),
    value("snmpv3_privpassphrase")("SNMPPrivacyPassphrase")("SNMPv3 priv passphrase. Used only by SNMPv3 items."),
    SNMPV3PrivProtocol.meta("snmpv3_privprotocol")("SNMPPrivacyProtocol"),
    SNMPV3SecurityLevel.meta("snmpv3_securitylevel")("SNMPSecurityLevel"),
    value("snmpv3_securityname")("SNMPSecurityName")("SNMPv3 security name. Used only by SNMPv3 items."),
    EnabledEnumZeroPositive.metaWithDesc("status")("status","enabled")("Status of the item."),
    value("trapper_hosts")("allowedHosts","trapperHosts")("Allowed hosts. Used only by trapper items."),
    value("trends")("trends")("""Number of days to keep item's trends data.
                                |Default: 365.""".stripMargin),
    value("units")("units")("Value units."),
    value("username")("username")("""Username for authentication. Used by simple check, SSH, Telnet, database monitor and JMX items.
                                    |Required by SSH and Telnet items.""".stripMargin)
  ) _
}
