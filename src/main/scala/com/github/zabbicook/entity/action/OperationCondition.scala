package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityState
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop.{EntityCompanionMetaHelper, EnumProp, IntEnumPropCompanion, IntProp}
import play.api.libs.json.{Format, Json}

sealed abstract class OperationConditionType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object OperationConditionType extends IntEnumPropCompanion[OperationConditionType] {
  override val values: Seq[OperationConditionType] = Seq(eventAck,unknown)
  override val description: String = "Type of condition."
  case object eventAck extends OperationConditionType(14, "Event acknowledged")
  case object unknown extends OperationConditionType(-1, "unknown")
}

sealed abstract class OperationConditionOperator(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object OperationConditionOperator extends IntEnumPropCompanion[OperationConditionOperator] {
  override val values: Seq[OperationConditionOperator] = Seq(equal,unknown)
  override val description: String = "Condition operator."
  case object equal extends OperationConditionOperator(0, "(default) =")
  case object unknown extends OperationConditionOperator(-1, "unknown")
}

case class OperationCondition[S <: EntityState](
  conditiontype: OperationConditionType = OperationConditionType.eventAck,
  value: String,
  operator: Option[OperationConditionOperator] = Some(OperationConditionOperator.equal)
)

object OperationCondition extends EntityCompanionMetaHelper {

  implicit val format: Format[OperationCondition[Stored]] = Json.format[OperationCondition[Stored]]

  implicit val format2: Format[OperationCondition[NotStored]] = Json.format[OperationCondition[NotStored]]

  override val meta = entity("The action operation condition object defines a condition that must be met to perform the current operation.")(
    value("conditiontype")("conditionType")("(retuired) Type of conditioin"),
    value("value")("value")("(required)	string	Value to compare with. Currently possible values are \"0\" - not acknowledged, or \"1\" - acknowledged."),
    OperationConditionOperator.meta("operator")("operator")
  ) _
}
