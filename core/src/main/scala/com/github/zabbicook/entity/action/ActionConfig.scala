package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop.{EnabledEnum, EnabledEnumZeroPositive, EntityCompanionMetaHelper, IntProp}

case class OpMessageConfig(
  default_msg: Option[EnabledEnumZeroPositive],
  mediaType: Option[String],
  message: Option[String],
  subject: Option[String]
) {
  def toNotStored: OperationMessage = {
    OperationMessage(default_msg = default_msg, message = message, subject = subject)
  }
}

object OpMessageConfig extends EntityCompanionMetaHelper {
  override def meta = entity(
    """Object containing the data about the message sent by the operation.
      |Required for message operations.""".stripMargin)(
    EnabledEnumZeroPositive.metaWithDesc("default_msg")("defaultMessage")(
      """Whether to use the default action message text and subject.
        |true - (default) use the data from the operation;
        |false - use the data from the action.""".stripMargin),
    value("mediaType")("mediaType")("Name of the media type that will be used to send the message."),
    value("message")("message")("Operation message text. It is used when defaultMessage is false."),
    value("subject")("subject")("Operation message subject. It is used when defaultMessage is false.")
  ) _
}

trait ActionOperationConfigHelper {
  protected[this] def commonMetaEntity = Seq(
    OperationType.meta("operationtype")("type","operationType"),
    OperationEvalType.meta("evaltype")("operationCondition"),
    OpMessageConfig.optional("message"),
    array("opmessage_grp")("sendToUserGroups")(
      """User groups to send messages to.
        |Required for message operations if 'sendToUsers' is not set. """.stripMargin),
    array("opmessage_usr")("sendToUsers")(
      """Users to send messages to.
        |Required for message operations if 'sendToUserGroups' is not set. """.stripMargin)
  )
}

case class ActionOperationConfig(
  operationtype: OperationType,
  esc_period: Option[IntProp],
  esc_step_from: Option[IntProp],
  esc_step_to: Option[IntProp],
  evaltype: Option[OperationEvalType],
  message: Option[OpMessageConfig],
  opmessage_grp: Option[Seq[String]],
  opMessageUser: Option[Seq[String]]
) {
  def toNotStored(messageGroups: Option[Seq[OpMessageGroup]], messageUsers: Option[Seq[OpMessageUser]]): ActionOperation[NotStored] = {
    ActionOperation[NotStored](
      operationtype = operationtype,
      esc_period = esc_period,
      esc_step_from = esc_step_from,
      esc_step_to = esc_step_to,
      evaltype = evaltype,
      opmessage = message.map(_.toNotStored),
      opmessage_grp = messageGroups,
      opmessage_usr = messageUsers
    )
  }
}

object ActionOperationConfig extends EntityCompanionMetaHelper with ActionOperationConfigHelper{
  private[this] val metaEntities = commonMetaEntity ++ Seq(
    value("esc_period")("stepDuration")("""Duration of an escalation step in seconds. Must be greater than 60 seconds.
                                          |If set to 0, the default action escalation period will be used.
                                          |Default: 0.""".stripMargin),
    value("esc_step_from")("stepFrom")("Step to start escalation from. Default: 1."),
    value("esc_step_to")("stepTo")("Step to end escalation at. Default: 1.")
  )
  override val meta = entity("The action operation object defines an operation that will be performed when an action is executed.")(
    metaEntities:_*
  ) _
}

case class RecoveryActionOperationConfig(
  operationtype: RecoveryOperationType,
  message: Option[OpMessageConfig],
  opmessage_grp: Option[Seq[String]],
  opMessageUser: Option[Seq[String]]
) {
  def toNotStored(messageGroups: Option[Seq[OpMessageGroup]], messageUsers: Option[Seq[OpMessageUser]]): RecoveryActionOperation[NotStored] = {
    RecoveryActionOperation[NotStored](
      operationtype = operationtype,
      opmessage = message.map(_.toNotStored),
      opmessage_grp = messageGroups,
      opmessage_usr = messageUsers
    )
  }
}

object RecoveryActionOperationConfig extends EntityCompanionMetaHelper with ActionOperationConfigHelper{
  private[this] val metaEntities = commonMetaEntity
  override val meta = entity("""The action recovery operation object defines an operation that will be performed when a problem is resolved.
                               |Recovery operations are possible for trigger actions and internal actions.
                               |(Zabbix version >= 3.2.x)""".stripMargin)(
    metaEntities:_*
  ) _
}

case class ActionConfig(
  name: String,
  esc_period: IntProp,
  eventsource: EventSource,
  def_longdata: Option[String],
  def_shortdata: Option[String],
  r_longdata: Option[String],
  r_shortdata: Option[String],
  recovery_msg: Option[EnabledEnum], // ver <= 3.0.x
  status: Option[EnabledEnum],
  filter: Option[ActionFilter[NotStored]],
  operations: Option[Seq[ActionOperationConfig]],
  recoveryOperations: Option[Seq[RecoveryActionOperationConfig]] // Ver >= 3.2.x
) {
  def toNotStoredAction: Action[NotStored] = {
    Action[NotStored](
      esc_period = esc_period,
      eventsource = eventsource,
      name = name,
      def_longdata = def_longdata,
      def_shortdata = def_shortdata,
      r_longdata = r_longdata,
      r_shortdata = r_shortdata,
      recovery_msg = recovery_msg,
      status = status
    )
  }
}

object ActionConfig extends EntityCompanionMetaHelper {

  override val meta = entity("The action object.")(
    value("name")("name")("(required)	string	Name of the action."),
    value("esc_period")("operationStep","duration")("(required)	integer	Default operation step duration. Must be greater than 60 seconds."),
    EventSource.metaWithDesc("eventsource")("event")("(required) Type of events that the action will handle."),
    value("def_longdata")("message", "defaultMessage")("Problem message text."),
    value("def_shortdata")("subject", "defaultSubject")("Problem message subject."),
    value("r_longdata")("recoveryMessage")("Recovery message text."),
    value("r_shortdata")("recoverySubject")("Recovery message subject"),
    EnabledEnum.metaWithDesc("recovery_msg")("recoveryMessageEnabled")("Whether recovery messages are enabled. (Zabbix version <= 3.0.x)"), // TODO: validate with a version
    EnabledEnum.metaWithDesc("status")("enabled")("Whether the action is enabled or disabled."),
    ActionFilter.optional("filter"),
    arrayOf("operations")(ActionOperationConfig.optional("operations")),
    arrayOf("recoveryOperations")(RecoveryActionOperationConfig.optional("recoveryOperations")) // TODO: validate with a version
  ) _
}
