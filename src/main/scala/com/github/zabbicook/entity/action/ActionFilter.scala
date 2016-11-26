package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop._
import com.github.zabbicook.entity.trigger.Severity
import com.github.zabbicook.entity.{EntityState, PropCompare}
import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed abstract class ActionFilterConditionType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object ActionFilterConditionType extends IntEnumPropCompanion[ActionFilterConditionType] {
  override val values: Seq[ActionFilterConditionType] = Seq(
    hostGroup,
    host,
    trigger,
    triggerName,
    triggerSeverity,
    triggerValue,
    timePeriod,
    hostIP,
    descoveredServiceType,
    discoveredServicePort,
    discoveredStatus,
    uptimeOrDowntimeDuration,
    receivedValue,
    hostTemplate,
    application,
    maintenanceStatus,
    discoveryRule,
    discoveryCheck,
    proxy,
    discoveryObject,
    hostName,
    eventType,
    hostMetadata,
    eventTag,
    eventTagValue,
    unknown)
  override val description: String = "Type of filter condition."
  case object hostGroup extends ActionFilterConditionType(0 , "host group (possible for trigger and internal actions)")
  case object host extends ActionFilterConditionType(1 , "host (possible for trigger and internal actions)")
  case object trigger extends ActionFilterConditionType(2 , "trigger (possible for trigger actions)")
  case object triggerName extends ActionFilterConditionType(3 , "trigger name (possible for trigger actions)")
  case object triggerSeverity extends ActionFilterConditionType(4 , "trigger severity (possible for trigger actions). " +
    "For this type, set a number according to the level at the 'value'." + Severity.possibleValues.map(s => s"${s.zabbixValue.value} - ${s.toString}").mkString(", "))
  case object triggerValue extends ActionFilterConditionType(5 , "trigger value (possible for trigger actions) (Zabbix Ver <= 3.0.x)")
  case object timePeriod extends ActionFilterConditionType(6 , "time period (possible for trigger actions)")
  case object hostIP extends ActionFilterConditionType(7 , "host IP (possible for discovery actions)")
  case object descoveredServiceType extends ActionFilterConditionType(8 , "discovered service type (possible for discovery actions)")
  case object discoveredServicePort extends ActionFilterConditionType(9 , "discovered service port (possible for discovery actions)")
  case object discoveredStatus extends ActionFilterConditionType(10 , "discovery status (possible for discovery actions)")
  case object uptimeOrDowntimeDuration extends ActionFilterConditionType(11 , "uptime or downtime duration (possible for discovery actions)")
  case object receivedValue extends ActionFilterConditionType(12 , "received value (possible for discovery actions)")
  case object hostTemplate extends ActionFilterConditionType(13 , "host template (possible for trigger and internal actions)")
  case object application extends ActionFilterConditionType(15 , "application (possible for trigger and internal actions)")
  case object maintenanceStatus extends ActionFilterConditionType(16 , "maintenance status. (possible for trigger actions)")
  case object discoveryRule extends ActionFilterConditionType(18 , "discovery rule (possible for discovery actions)")
  case object discoveryCheck extends ActionFilterConditionType(19 , "discovery check (possible for discovery actions)")
  case object proxy extends ActionFilterConditionType(20 , "proxy (possible for discovery and auto-registration actions)")
  case object discoveryObject extends ActionFilterConditionType(21 , "discovery object. (possible for discovery actions)")
  case object hostName extends ActionFilterConditionType(22 , "host name (possible for auto-registration actions)")
  case object eventType extends ActionFilterConditionType(23 , "event type. (possible for internal actions)")
  case object hostMetadata extends ActionFilterConditionType(24 , "host metadata. (possible for auto-registration actions)")
  case object eventTag extends ActionFilterConditionType(25 , "event tag (possible for trigger actions) (Zabbix Ver >= 3.2.x)")
  case object eventTagValue extends ActionFilterConditionType(26 , "event tag value (possible for trigger actions) (Zabbix Ver >= 3.2.x)")
  case object unknown extends ActionFilterConditionType(-1, "unknown")
}

sealed abstract class FilterConditionOperator(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object FilterConditionOperator extends IntEnumPropCompanion[FilterConditionOperator] {
  override val values: Seq[FilterConditionOperator] = Seq(
    equal,
    notEqual,
    like,
    notLike,
    in,
    greaterEqual,
    lessEqual,
    notIn,
    unknown)
  override val description: String = "Filter condition operator."
  case object equal extends FilterConditionOperator(0 , "= (default)")
  case object notEqual extends FilterConditionOperator(1 , "<>")
  case object like extends FilterConditionOperator(2 , "like")
  case object notLike extends FilterConditionOperator(3 , "not like")
  case object in extends FilterConditionOperator(4 , "in")
  case object greaterEqual extends FilterConditionOperator(5 , ">=")
  case object lessEqual extends FilterConditionOperator(6 , "<=")
  case object notIn extends FilterConditionOperator(7 , "not in")
  case object unknown extends FilterConditionOperator(-1, "unknown")
}

case class ActionFilterCondition[S <: EntityState](
  conditiontype: ActionFilterConditionType,
  value: String,
  value2: Option[String] = None,
  formulaid: Option[String] = None,
  operator: Option[FilterConditionOperator] = None
) extends PropCompare {
  def isSame[T >: S <: Stored](constant: ActionFilterCondition[NotStored]): Boolean = {
    val sameValue2 = isSameProp(value2, constant.value2, defValue = "")
    val sameOperator = isSameProp(operator, constant.operator, FilterConditionOperator.equal)
    (conditiontype == constant.conditiontype) && (value == constant.value) && sameValue2 && sameOperator
  }
}

object ActionFilterCondition extends EntityCompanionMetaHelper {
  implicit val format: Format[ActionFilterCondition[Stored]] = Json.format[ActionFilterCondition[Stored]]
  implicit val format2: Format[ActionFilterCondition[NotStored]] = Json.format[ActionFilterCondition[NotStored]]

  override val meta = entity("Set of filter conditions to use for filtering results.")(
    ActionFilterConditionType.metaWithDesc("conditiontype")("type")("(required) " + ActionFilterConditionType.description),
    value("value")("value")("(required) Value to compare with."),
    value("value2")("value2")(s"Secondary value to compare with. Requried for trigger actions when condition type is '${ActionFilterConditionType.eventTagValue}'"),
    value("formulaid")("label","formulaid")(
      """Arbitrary unique ID that is used to reference the condition from a custom expression.
        |Can only contain capital-case letters.
        |The ID must be defined by the user when modifying filter conditions, but will be generated anew when requesting them afterward.
      """.stripMargin),
    FilterConditionOperator.meta("operator")("operator")
  ) _
}

sealed abstract class ActionFilterEvalType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object ActionFilterEvalType extends IntEnumPropCompanion[ActionFilterEvalType] {
  override val values: Seq[ActionFilterEvalType] = Seq(AndOr,And,Or,customExpression,unknown)
  override val description: String = s"""(required) Filter condition evaluation method.
                                      |When conditions is empty array this does not affect but it should be set '$AndOr'.""".stripMargin
  case object AndOr extends ActionFilterEvalType(0, "(default) AND / OR")
  case object And extends ActionFilterEvalType(1, "AND")
  case object Or extends ActionFilterEvalType(2, "OR")
  case object customExpression extends ActionFilterEvalType(3, "custom expression")
  case object unknown extends ActionFilterEvalType(-1, "unknown")
}

case class ActionFilter[S <: EntityState] (
  conditions: Seq[ActionFilterCondition[S]],
  evaltype: ActionFilterEvalType,
  formula: Option[String] = None
) {
  def shouldBeUpdated[T >: S <: Stored](constant: ActionFilter[NotStored]): Boolean = {
    val sameForumula = (formula, constant.formula) match {
      case (Some(s), Some(c)) if s == c => true
      case (Some(""), None) => true
      case (None, None) => true
      case _ => false
    }

    !(conditions.length == constant.conditions.length &&
      constant.conditions.forall(cond => conditions.exists(_.isSame[T](cond))) &&
      evaltype == constant.evaltype &&
      sameForumula)
  }
}

object ActionFilter extends EntityCompanionMetaHelper {
  implicit val format: Format[ActionFilter[Stored]] = (
    (__ \ "conditions").format[Seq[ActionFilterCondition[Stored]]] and
    (__ \ "evaltype").format[ActionFilterEvalType] and
    (__ \ "formula").formatNullable[String]
  )(ActionFilter.apply[Stored], unlift(unapply2[Stored]))

  def unapply2[S <: EntityState](arg: ActionFilter[S]): Option[(Seq[ActionFilterCondition[S]], ActionFilterEvalType, Option[String])] = {
    Option((arg.conditions, arg.evaltype, arg.formula))
  }

  implicit val format2: Format[ActionFilter[NotStored]] = (
    (__ \ "conditions").format[Seq[ActionFilterCondition[NotStored]]] and
    (__ \ "evaltype").format[ActionFilterEvalType] and
    (__ \ "formula").formatNullable[String]
  )(ActionFilter.apply[NotStored], unlift(unapply2[NotStored]))

  override val meta = entity("The action filter object defines a set of conditions that must be met to perform the configured action operations.")(
    arrayOf("conditions")(ActionFilterCondition.required("conditions")),
    ActionFilterEvalType.meta("evaltype")("type","typeOfCalculation"),
    value("formula")("customExpression","formula")(
      s"""User-defined expression to be used for evaluating conditions of filters with a '${ActionFilterEvalType.customExpression}'.
        |The expression must contain IDs that reference specific filter conditions by its formulaid(i.e. 'label').
        |The IDs used in the expression must exactly match the ones defined in the filter conditions:
        |no condition can remain unused or omitted.
        |Required for '${ActionFilterEvalType.customExpression}' filters.
      """.stripMargin)
  ) _

  def empty[T <: EntityState]: ActionFilter[T] = ActionFilter[T](
    conditions = Seq(),
    evaltype = ActionFilterEvalType.AndOr,
    formula = None
  )
}
