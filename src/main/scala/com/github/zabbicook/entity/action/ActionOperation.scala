package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.IntProp
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json._

trait ActionOperationHelper {
}

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

  def isMediaTypeIdSet: Boolean = opmessage.map(_.isMediaTypeIdSet).getOrElse(true)

  def isSame[T >: S <: Stored](constant: ActionOperation[NotStored]): Boolean = {
    require(isMediaTypeIdSet && constant.isMediaTypeIdSet)
    operationtype == constant.operationtype &&
      isSameProp(esc_period, constant.esc_period, defValue = IntProp(0)) &&
      isSameProp(esc_step_from, constant.esc_step_from, defValue = IntProp(1)) &&
      isSameProp(esc_step_to, constant.esc_step_to, defValue = IntProp(1)) &&
      isSameProp(evaltype, constant.evaltype, defValue = OperationEvalType.AndOr) &&
      isSameProp(opmessage, constant.opmessage, OperationMessage.default, (a:OperationMessage,b:OperationMessage) => a.isSame(b)) &&
      isSameProp(opmessage_grp, constant.opmessage_grp, defValue = Seq.empty[OpMessageGroup], (a: Seq[OpMessageGroup],b:Seq[OpMessageGroup]) => a.toSet == b.toSet) &&
      isSameProp(opmessage_usr, constant.opmessage_usr, defValue = Seq.empty[OpMessageUser], (a: Seq[OpMessageUser],b:Seq[OpMessageUser]) => a.toSet == b.toSet)
  }
}

object ActionOperation {
  implicit val format: Format[ActionOperation[Stored]] = Json.format[ActionOperation[Stored]]

  implicit val format2: Format[ActionOperation[NotStored]] = Json.format[ActionOperation[NotStored]]

  // Zabbix version 3.2 or later fails in action.update query with parameter which contains an operationid.
  val transformerForUpdate: Reads[JsObject] = (__ \ "operationid").json.prune
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

  def isMediaTypeIdSet: Boolean = opmessage.map(_.isMediaTypeIdSet).getOrElse(true)

  def isSame[T >: S <: Stored](constant: RecoveryActionOperation[NotStored]): Boolean = {
    require(isMediaTypeIdSet && constant.isMediaTypeIdSet)
    operationtype == constant.operationtype &&
      isSameProp(opmessage, constant.opmessage, defValue = null, (a:OperationMessage,b:OperationMessage) => a.isSame(b)) &&
      isSameProp(opmessage_grp, constant.opmessage_grp, defValue = Seq.empty[OpMessageGroup], (a: Seq[OpMessageGroup],b:Seq[OpMessageGroup]) => a.toSet == b.toSet) &&
      isSameProp(opmessage_usr, constant.opmessage_usr, defValue = Seq.empty[OpMessageUser], (a: Seq[OpMessageUser],b:Seq[OpMessageUser]) => a.toSet == b.toSet)
  }
}

object RecoveryActionOperation {
  implicit val format: Format[RecoveryActionOperation[Stored]] = Json.format[RecoveryActionOperation[Stored]]
  implicit val format2: Format[RecoveryActionOperation[NotStored]] = Json.format[RecoveryActionOperation[NotStored]]

  // Zabbix version 3.2 or later fails in action.update query with parameter which contains an operationid.
  val transformerForUpdate: Reads[JsObject] = (__ \ "operationid").json.prune
}
