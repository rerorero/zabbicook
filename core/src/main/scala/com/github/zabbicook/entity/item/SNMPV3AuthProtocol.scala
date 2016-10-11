package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.prop._

sealed abstract class SNMPV3AuthProtocol(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object SNMPV3AuthProtocol extends IntEnumPropCompanion[SNMPV3AuthProtocol] {
  override val values: Set[SNMPV3AuthProtocol] = Set(MD5,SHA,unknown)
  override val description = "SNMPv3 authentication protocol. Used only by SNMPv3 items. "
  case object MD5 extends SNMPV3AuthProtocol(0, "(default) MD5")
  case object SHA extends SNMPV3AuthProtocol(1, "SHA")
  case object unknown extends SNMPV3AuthProtocol(-1, "unknown")
}

sealed abstract class SNMPV3PrivProtocol(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object SNMPV3PrivProtocol extends IntEnumPropCompanion[SNMPV3PrivProtocol] {
  override val values: Set[SNMPV3PrivProtocol] = Set(DES,AES,unknown)
  override val description = "SNMPv3 privacy protocol. Used only by SNMPv3 items."
  case object DES extends SNMPV3PrivProtocol(0, "DES")
  case object AES extends SNMPV3PrivProtocol(1, "AES")
  case object unknown extends SNMPV3PrivProtocol(-1, "unknown")
}

sealed abstract class SNMPV3SecurityLevel(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object SNMPV3SecurityLevel extends IntEnumPropCompanion[SNMPV3SecurityLevel] {
  override val values: Set[SNMPV3SecurityLevel] = Set(
    noAuthNoPriv,authNoPriv,authPriv,unknown
  )
  override val description = "SNMPv3 security level. Used only by SNMPv3 items."
  case object noAuthNoPriv extends SNMPV3SecurityLevel(0, "noAuthNoPriv")
  case object authNoPriv extends SNMPV3SecurityLevel(1, "authNoPriv")
  case object authPriv extends SNMPV3SecurityLevel(2, "authPriv")
  case object unknown extends SNMPV3SecurityLevel(-1, "unknown")
}
