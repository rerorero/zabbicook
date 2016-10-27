package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop.{EntityCompanionMetaHelper, EnumProp, IntEnumPropCompanion, IntProp}
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

sealed abstract class OperationCommandType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object OperationCommandType extends IntEnumPropCompanion[OperationCommandType] {
  override val values: Set[OperationCommandType] = Set(customScript,IPMI,SSH,Telnet,unknown)
  override val description: String = "Type of operation command."
  case object customScript extends OperationCommandType(0, "custom script")
  case object IPMI extends OperationCommandType(1,"IPMI")
  case object SSH extends OperationCommandType(2, "SSH")
  case object Telnet extends OperationCommandType(3, "Telnet")
  // case object globalScript extends OperationCommandType(4, "global script") TODO: support global scripts
  case object unknown extends OperationCommandType(-1, "unknown")
}

sealed abstract class OperationAuthType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object OperationAuthType extends IntEnumPropCompanion[OperationAuthType] {
  override val values: Set[OperationAuthType] = Set(password,publicKey,unknown)
  override val description: String = "Authentication method used for SSH commands."
  case object password extends OperationAuthType(0, "password")
  case object publicKey extends OperationAuthType(1,"public key")
  case object unknown extends OperationAuthType(-1, "unknown")
}

sealed abstract class ExecuteOn(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object ExecuteOn extends IntEnumPropCompanion[ExecuteOn] {
  override val values: Set[ExecuteOn] = Set(agent,server,unknown)
  override val description: String = "Target on which the custom script operation command will be executed."
  case object agent extends ExecuteOn(0, "Zabbix agent")
  case object server extends ExecuteOn(1,"Zabbix server")
  case object unknown extends ExecuteOn(-1, "unknown")
}

case class OperationCommand[S <: EntityState](
  operationid: EntityId,
  command: Option[String],
  `type`: OperationCommandType,
  authMethod: Option[OperationAuthType],
  executeOn: Option[ExecuteOn],
  password: Option[String],
  port: Option[String],
  privatekey: Option[String],
  publickey: Option[String],
  //scriptid: EntityId, TODO: support global scripts
  username: Option[String]
) extends Entity[S] {
  override protected[this] def id: EntityId = operationid
}

object OperationCommand extends EntityCompanionMetaHelper {

  implicit val format: Format[OperationCommand[Stored]] = Json.format[OperationCommand[Stored]]

  implicit val format2: Format[OperationCommand[NotStored]] = Json.format[OperationCommand[NotStored]]

  override val meta = entity("""The operation command object contains data about the command that will be run by the operation.
                               |Required for remote command operations.""".stripMargin)(
    readOnly("operationid"),
    value("command")("command","commands")(s"Command to run. Required when type IN (${OperationCommandType.customScript},${OperationCommandType.IPMI},${OperationCommandType.SSH},${OperationCommandType.Telnet})."),
    OperationCommandType.metaWithDesc("type")("type")("(required) " + OperationCommandType.description),
    OperationAuthType.metaWithDesc("authType")("authMethod")(OperationAuthType.description + " Required for SSH commands."),
    ExecuteOn.metaWithDesc("execute_on")("executeOn")(ExecuteOn.description + " Required for custom script commands."),
    value("password")("password")("Password used for SSH commands with password authentication and Telnet commands."),
    value("port")("port")("Port number used for SSH and Telnet commands. (String value)"),
    value("privatekey")("privateKey")("""Name of the private key file used for SSH commands with public key authentication.
                                        |Required for SSH commands with public key authentication.""".stripMargin),
    value("publickey")("publicKey")("""Name of the public key file used for SSH commands with public key authentication.
                                      |Required for SSH commands with public key authentication.""".stripMargin),
    //readOnly("scriptid"), // ID of the script used for global script commands. TODO: support global script
    value("username")("userName")("""User name used for authentication.
                                    |Required for SSH and Telnet commands.""".stripMargin)
  ) _
}

case class OpCommandGroup[S <: EntityState](
  opcommand_grpid: EntityId,
  operationid: EntityId,
  groupid: EntityId
) extends Entity[S] {
  override protected[this] def id: EntityId = opcommand_grpid
}

case class OpCommandHost[S <: EntityState](
  opcommand_hostid: EntityId,
  operationid: EntityId,
  hostid: EntityId
) extends Entity[S] {
  override protected[this] def id: EntityId = opcommand_hostid
}

