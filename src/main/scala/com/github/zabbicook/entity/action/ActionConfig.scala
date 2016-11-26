package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop.{EnabledEnum, EnabledEnumZeroPositive, EntityCompanionMetaHelper, IntProp}

case class OpMessageConfig(
  default_msg: Option[EnabledEnum]=None,
  mediaType: Option[String]=None,
  message: Option[String]=None,
  subject: Option[String]=None
) {
  def toNotStored(mediatypeId: Option[StoredId]): OperationMessage = {
    OperationMessage(default_msg = default_msg, mediatypeid = mediatypeId, message = message, subject = subject)
  }
}

object OpMessageConfig extends EntityCompanionMetaHelper {
  override def meta = entity(
    """Object containing the data about the message sent by the operation.
      |Required for message operations.""".stripMargin)(
    EnabledEnum.metaWithDesc("default_msg")("defaultMessage")(
      """Whether to use the default action message text and subject.
        |When set 'true', use the data from the operation;
        |When set 'false', use the data from the action.
        |Default: true.""".stripMargin),
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
  esc_period: Option[IntProp] = None,
  esc_step_from: Option[IntProp] = None,    // TODO: validate: esc_step_from and esc_step_to must be set together.
  esc_step_to: Option[IntProp] = None,
  evaltype: Option[OperationEvalType] = None,
  message: Option[OpMessageConfig] = None,
  opmessage_grp: Option[Seq[String]] = None,
  opmessage_usr: Option[Seq[String]] = None
) {
  def toNotStored(messageGroups: Option[Seq[OpMessageGroup]], messageUsers: Option[Seq[OpMessageUser]], mediaTypeId: Option[StoredId]): ActionOperation[NotStored] = {
    ActionOperation[NotStored](
      operationtype = operationtype,
      esc_period = esc_period,
      esc_step_from = esc_step_from,
      esc_step_to = esc_step_to,
      evaltype = evaltype,
      opmessage = message.map(_.toNotStored(mediaTypeId)),
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
  message: Option[OpMessageConfig] = None,
  opmessage_grp: Option[Seq[String]] = None,
  opmessage_usr: Option[Seq[String]] = None
) {
  def toNotStored(
    messageGroups: Option[Seq[OpMessageGroup]],
    messageUsers: Option[Seq[OpMessageUser]],
    mediatypeId: Option[StoredId]
  ): RecoveryActionOperation[NotStored] = {
    RecoveryActionOperation[NotStored](
      operationtype = operationtype,
      opmessage = message.map(_.toNotStored(mediatypeId)),
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

/**
  * configurations of action + action filter + operations + recovery operations
  */
case class ActionConfig(
  name: String,
  esc_period: IntProp,
  eventsource: EventSource,
  def_longdata: Option[String] = None,
  def_shortdata: Option[String] = None,
  r_longdata: Option[String] = None,
  r_shortdata: Option[String] = None,
  recovery_msg: Option[EnabledEnum] = None, // ver <= 3.0.x
  status: Option[EnabledEnum] = None,
  filter: ActionFilter[NotStored],
  operations: Seq[ActionOperationConfig],
  recoveryOperations: Option[Seq[RecoveryActionOperationConfig]] = None // Ver >= 3.2.x
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
  override val meta = entity("Action object.")(
    value("name")("name")("(required)	string	Name of the action."),
    value("esc_period")("defaultStepDuration","stepDuration")("(required)	integer	Default operation step duration. Must be greater than 60 seconds."),
    EventSource.metaWithDesc("eventsource")("event")("(required)(constant) Type of events that the action will handle. This value is not changeable."),
    value("def_longdata")("message", "defaultMessage")("Problem message text."),
    value("def_shortdata")("subject", "defaultSubject")("Problem message subject."),
    value("r_longdata")("recoveryMessage")("Recovery message text."),
    value("r_shortdata")("recoverySubject")("Recovery message subject"),
    EnabledEnum.metaWithDesc("recovery_msg")("recoveryMessageEnabled")("Whether recovery messages are enabled. (Zabbix version <= 3.0.x)"), // TODO: validate with a version
    EnabledEnum.metaWithDesc("status")("enabled")("Whether the action is enabled or disabled."),
    ActionFilter.required("filter"),
    arrayOf("operations")(ActionOperationConfig.required("operations")),
    arrayOf("recoveryOperations")(RecoveryActionOperationConfig.optional("recoveryOperations")) // TODO: validate with a version
  ) _
}
