package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.prop.{EnumProp, IntEnumPropCompanion, IntProp}

sealed abstract class OperationType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object OperationType extends IntEnumPropCompanion[OperationType] {
  override val values: Set[OperationType] = Set(sendMessage,
    /*
    remoteCommand,addHost,removeHost,
    addToHostGroup,removeFromHostGroup,linkToTemplate, unlinkFromTemplate,
    enableHost,disableHost,setInventoryMode,
    */
    unknown)
  override val description: String = "Type of operation."
  case object sendMessage extends OperationType(0, "send message")
  // TODO: support the followings.
  // case object remoteCommand extends OperationType(1,"remote command")
//  case object addHost extends OperationType(2,"add host")
//  case object removeHost extends OperationType(3 , "remove host")
//  case object addToHostGroup extends OperationType(4 , "add to host group")
//  case object removeFromHostGroup extends OperationType(5 , "remove from host group")
//  case object linkToTemplate extends OperationType(6 , "link to template")
//  case object unlinkFromTemplate extends OperationType(7 , "unlink from template")
//  case object enableHost extends OperationType(8 , "enable host")
//  case object disableHost extends OperationType(9 , "disable host")
  //  case object setInventoryMode extends OperationType(10 , "set host inventory mode")
  case object unknown extends OperationType(-1, "unknown")
}

sealed abstract class RecoveryOperationType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object RecoveryOperationType extends IntEnumPropCompanion[RecoveryOperationType] {
  override val values: Set[RecoveryOperationType] = Set(sendMessage,/*remoteCommand,*/sendRecoveryMessage,unknown)
  override val description: String = "Type of recovery operation."
  case object sendMessage extends RecoveryOperationType(0, "send message")
  // TODO: support the followings
//  case object remoteCommand extends RecoveryOperationType(1,"remote command (only for trigger actions)")
  case object sendRecoveryMessage extends RecoveryOperationType(11,"send recovery message.")
  case object unknown extends RecoveryOperationType(-1, "unknown")
}
