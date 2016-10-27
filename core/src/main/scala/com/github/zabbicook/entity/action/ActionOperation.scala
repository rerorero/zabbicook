package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.IntProp
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

case class ActionOperation[S <: EntityState] (
  operationid: EntityId = NotStoredId,
  operationtype: OperationType,
  // actionid: EntityId,
  esc_period: Option[IntProp],
  esc_step_from: Option[IntProp],
  esc_step_to: Option[IntProp],
  evaltype: Option[OperationEvalType],
//  opcommand: Option[OperationCommand[S]],         TODO: support 'remote command' operations
//  opcommand_grp: Option[Seq[OpCommandGroup[S]]],
//  opcommand_hst: Option[Seq[OpCommandHost[S]]],
//  opconditions: Option[Seq[OperationCondition]],
  //opgroup	    // unhandled array	Host groups to add hosts to. Required for “add to host group” and “remove from host group” operations.
  opmessage: Option[OperationMessage],
  opmessage_grp: Option[Seq[OpMessageGroup]],
  opmessage_usr: Option[Seq[OpMessageUser]]
  //optemplate	// unhandled array	Templates to link the hosts to to. Required for “link to template” and “unlink from template” operations.
  // opinventory	// unhandled object	Inventory mode set host to. Required for “Set host inventory mode” operations.

) extends Entity[S] {
  override protected[this] def id: EntityId = operationid
  def toStored(id: StoredId, storedMessage: Option[OperationMessage]): ActionOperation[Stored] = {
    copy(operationid = id, opmessage = storedMessage)
  }
}

object ActionOperation {
  implicit val format: Format[ActionOperation[Stored]] = Json.format[ActionOperation[Stored]]
  implicit val format2: Format[ActionOperation[NotStored]] = Json.format[ActionOperation[NotStored]]
}

case class RecoveryActionOperation[S <: EntityState] (
  operationid: EntityId = NotStoredId,
  operationtype: RecoveryOperationType,
  // actionid: EntityId, read only?
  //  opcommand: Option[OperationCommand[S]],         TODO: support 'remote command' operations
  //  opcommand_grp: Option[Seq[OpCommandGroup[S]]],
  //  opcommand_hst: Option[Seq[OpCommandHost[S]]],
  //  opconditions: Option[Seq[OperationCondition]],
  //opgroup	    // unhandled array	Host groups to add hosts to. Required for “add to host group” and “remove from host group” operations.
  opmessage: Option[OperationMessage],
  opmessage_grp: Option[Seq[OpMessageGroup]],
  opmessage_usr: Option[Seq[OpMessageUser]]
) extends Entity[S] {
  override protected[this] def id: EntityId = operationid
  def toStored(id: StoredId, storedMessage: Option[OperationMessage]): RecoveryActionOperation[Stored] = {
    copy(operationid = id, opmessage = storedMessage)
  }
}

object RecoveryActionOperation {
  implicit val format: Format[RecoveryActionOperation[Stored]] = Json.format[RecoveryActionOperation[Stored]]
  implicit val format2: Format[RecoveryActionOperation[NotStored]] = Json.format[RecoveryActionOperation[NotStored]]
}
