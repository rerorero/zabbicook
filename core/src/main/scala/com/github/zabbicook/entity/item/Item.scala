package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity._
import com.github.zabbicook.entity.item.Item.ItemEnabled
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{Format, JsObject, Json}

import ai.x.play.json.Jsonx

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/item/object
  */
case class Item[S <: EntityState](
  itemid: EntityId = NotStoredId, // read only
  delay: Int,                     // required Update interval of the item in seconds.
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
  formula: Option[Double] = None,     // integer/float	Custom multiplier.
  history: Option[Int] = None,	      // integer	Number of days to keep item's history data.
  inventory_link: Option[Int] = None, //	integer	ID of the host inventory field that is populated by the item.
                                      // @see https://www.zabbix.com/documentation/3.2/manual/api/reference/host/object#host_inventory
  ipmi_sensor: Option[String]=None,   // string	IPMI sensor. Used only by IPMI items.
  //lastclock	                        // unhandled timestamp	(readonly) Time when the item was last updated.
  //lastns	                          // unhandled integer	(readonly) Nanoseconds when the item was last updated.
  //lastvalue                         // unhandled	(readonly) Last value of the item.
  logtimefmt: Option[String]=None,    // string	Format of the time in log entries. Used only by log items.
  //mtime	                            // unhandled timestamp	Time when the monitored log file was last updated. Used only by log items.
  multiplier: Option[Int]=None,       // integer	Whether to use a custom multiplier.
  params: Option[String]=None,        // string	Additional parameters depending on the type of the item:
  password: Option[String]=None,	    // string	Password for authentication. Used by simple check, SSH, Telnet, database monitor and JMX items.
  port: Option[String]=None,          // string	Port monitored by the item. Used only by SNMP items.
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
  status: Option[ItemEnabled]=None,   //	integer	Status of the item.
  //templateid	                      // unhandled string	(readonly) ID of the parent template item.
  trapper_hosts: Option[String]=None, //	string	Allowed hosts. Used only by trapper items.
  trends: Option[Int]=None,           // 	integer	Number of days to keep item's trends data.
  units: Option[String]=None,         // 	string	Value units.
  username: Option[String]=None       //	string	Username for authentication. Used by simple check, SSH, Telnet, database monitor and JMX items.
  //valuemapid: EntityId	            // unhandled TODO string	ID of the associated value map.
) extends Entity[S] {

  override protected[this] val id: EntityId = itemid

  def toStored(id: StoredId): Item[Stored] = copy(itemid = id)

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
    require(name == constant.name)
    delay != constant.delay ||
    hostid != constant.hostid ||
    `key_` != constant.`key_` ||
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

object Item {
  type ItemEnabled = EnabledEnumZeroPositive

  // tracing https://github.com/playframework/playframework/issues/3174
  implicit lazy val format: Format[Item[Stored]] = Jsonx.formatCaseClass[Item[Stored]]

  implicit lazy val format2: Format[Item[NotStored]] = Jsonx.formatCaseClass[Item[NotStored]]

  implicit val hocon: HoconReads[Item[NotStored]] = {
    val reads = for {
      delay <- required[Int]("delay")
      // hostid <- required[String]("host")  provided by the Template name
      key <- required[String]("key")
      name <- required[String]("name")
      typ <- required[ItemType]("type")
      valueType <- required[ValueType]("typeOfInformation")
      authType <- optional[AuthType]("authMethod")
      dataType <- optional[DataType]("dataType")
      delta <- optional[Delta]("delta")
      description <- optional[String]("description")
      formula <- optional[Double]("formula")
      history <- optional[Int]("historyPeriod")
      inventoryLink <- optional[Int]("hostInventoryField")
      ipmiSensor <- optional[String]("IPMISensor")
      logtimefmt <- optional[String]("logTimeFormat")
      multiplier <- optional[Int]("customMultiplier")
      params <- optional[String]("params") // TODO will be able to treat the alternative names
      password <- optional[String]("password")
      port <- optional[String]("port")
      privateKey <- optional[String]("privateKeyFile")
      publicKey <- optional[String]("publicKeyFile")
      snmpCommunity <- optional[String]("SNMPCommunity")
      snmpOid <- optional[String]("OID")
      snmpAuthPassPhrase <- optional[String]("SNMPAuthPassphrase")
      snmpAuthProtocol <- optional[SNMPV3AuthProtocol]("SNMPAuthProtocol")
      snmpContextName <- optional[String]("SNMPContextName")
      snmpPrivPassphrase <- optional[String]("SNMPPrivacyPassphrase")
      snmpPrivProtocol <- optional[SNMPV3PrivProtocol]("SNMPPrivacyProtocol")
      snmpSecurityLevel <- optional[SNMPV3SecurityLevel]("SNMPSecurityLevel")
      snmpSecurityName <- optional[String]("SNMPSecurityName")
      status <- optional[ItemEnabled]("enabled")
      trapperHost <- optional[String]("allowedHosts")
      trends <- optional[Int]("trendPeriod")
      units <- optional[String]("units")
      username <- optional[String]("username")
    } yield {
      Item[NotStored](
        NotStoredId,
        delay,
        NotStoredId,
        key,
        name,
        typ,
        valueType,
        authType,
        dataType,
        delta,
        description,
        formula,
        history,
        inventoryLink,
        ipmiSensor,
        logtimefmt,
        multiplier,
        params,
        password,
        port,
        privateKey,
        publicKey,
        snmpCommunity,
        snmpOid,
        snmpAuthPassPhrase,
        snmpAuthProtocol,
        snmpContextName,
        snmpPrivPassphrase,
        snmpPrivProtocol,
        snmpSecurityLevel,
        snmpSecurityName,
        status,
        trapperHost,
        trends,
        units,
        username
      )
    }
    reads.withAcceptableKeys(
      "delay",
      "key",
      "name",
      "type",
      "typeOfInformation",
      "authMethod",
      "dataType",
      "delta",
      "description",
      "formula",
      "historyPeriod",
      "hostInventoryField",
      "IPMISensor",
      "logTimeFormat",
      "customMultiplier",
      "params",
      "password",
      "port",
      "privateKeyFile",
      "publicKeyFile",
      "SNMPCommunity",
      "OID",
      "SNMPAuthPassphrase",
      "SNMPAuthProtocol",
      "SNMPContextName",
      "SNMPPrivacyPassphrase",
      "SNMPPrivacyProtocol",
      "SNMPSecurityLevel",
      "SNMPSecurityName",
      "enabled",
      "allowedHosts",
      "trendPeriod",
      "units",
      "username"
    )
  }
}
