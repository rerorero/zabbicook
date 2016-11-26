package com.github.zabbicook.entity.host

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.prop._
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}
import Meta._
import com.github.zabbicook.entity.EntityId.NotStoredId

sealed abstract class InterfaceType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object InterfaceType extends IntEnumPropCompanion[InterfaceType] {
  override val values: Seq[InterfaceType] = Seq(agent,SNMP,IPMI,JMX,unknown)
  override val description: String = "(required) Interface type."
  case object agent extends InterfaceType(1, "agent")
  case object SNMP extends InterfaceType(2, "SNMP")
  case object IPMI extends InterfaceType(3, "IPMI")
  case object JMX extends InterfaceType(4, "JMX")
  case object unknown extends InterfaceType(-1, "unknown")
}

sealed abstract class InterfaceUseIp(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object InterfaceUseIp extends IntEnumPropCompanion[InterfaceUseIp] {
  override val values: Seq[InterfaceUseIp] = Seq(dns,ip,unknown)
  override val description: String = "(required) Whether the connection should be made via IP."
  case object dns extends InterfaceUseIp(0, "connect using host DNS name")
  case object ip extends InterfaceUseIp(1, "connect using host IP address for this host interface.")
  case object unknown extends InterfaceUseIp(-1, "unknown")
}

case class HostInterface[S <: EntityState](
  interfaceid: EntityId = NotStoredId,
  dns: Option[String] = None, // Either dns or ip is required. TODO: validate
  // hostid: EntityId, unhandled
  ip: Option[String] = None, // Either dns or ip is required.
  main: EnabledEnum,
  port: String,
  `type`: InterfaceType,
  useip: InterfaceUseIp,
  bulk: Option[EnabledEnum] = None
) extends Entity[S] {

  override protected[this] def id: EntityId = interfaceid

  // Whether to treat as the identical interface
  def isIdentical[T <: EntityState](that: HostInterface[T]): Boolean = {
    `type` == that.`type` &&
      port == that.port &&
      useip == that.useip &&
      (useip match {
        case InterfaceUseIp.ip => ip == that.ip
        case InterfaceUseIp.dns => dns == that.dns
        case InterfaceUseIp.unknown => false
      })
  }

  def resolveDnsOrIp[T >: S <: NotStored]: HostInterface[NotStored] = {
    // According to the documents, empty string is required if none.
    copy(dns = dns.orElse(Some("")), ip = ip.orElse(Some("")))
  }

  def shouldBeUpdated[T >: S <: Stored](constant: HostInterface[NotStored]): Boolean = {
    require(isIdentical(constant))
    shouldBeUpdated(bulk, constant.bulk)
  }
}

object HostInterface extends EntityCompanionMetaHelper {
  implicit val format: Format[HostInterface[NotStored]] = Json.format[HostInterface[NotStored]]

  implicit val format2: Format[HostInterface[Stored]] = Json.format[HostInterface[Stored]]

  override val meta = entity("Host interface object")(
    readOnly("interfaceid"),
    value("dns")("dns")("""(required if DNS used) DNS name used by the interface.
                          |Can be empty if the connection is made via IP.""".stripMargin),
    value("ip")("ip")(
      """(required if IP used) IP address used by the interface.
        |Can be empty if the connection is made via DNS.
      """.stripMargin),
    EnabledEnum.metaWithDesc("main")("default","main")(
      """(required)	boolean	Whether the interface is used as default on the host." +
        |Only one interface of some type can be set as default on a host.""".stripMargin),
    value("port")("port")("""(required, String) Port number used by the interface. Can contain user macros."""),
    InterfaceType.meta("type")("type"),
    InterfaceUseIp.meta("useip")("connectTo","useIP"),
    EnabledEnum.metaWithDesc("bulk")("bulk","useBulk")("Whether to use bulk SNMP requests.")
  ) _
}
