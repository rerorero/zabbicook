package com.github.zabbicook.test

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.action._
import com.github.zabbicook.operation.Ops

trait TestActions extends TestUsers { self: UnitSpec =>

  protected[this] val filter0 = ActionFilter[NotStored](
    conditions = Seq(
      ActionFilterCondition(
        conditiontype = ActionFilterConditionType.triggerName,
        value = "trigger",
        operator = Some(FilterConditionOperator.equal)
      ),
      ActionFilterCondition(
        conditiontype = ActionFilterConditionType.triggerSeverity,
        value = "2",
        operator = Some(FilterConditionOperator.greaterEqual)
      )
    ),
    evaltype = ActionFilterEvalType.Or
  )

  protected[this] val filter1 = ActionFilter[NotStored](
    conditions = Seq(
      ActionFilterCondition(
        conditiontype = ActionFilterConditionType.triggerName,
        value = "trigger2",
        operator = Some(FilterConditionOperator.equal)
      )
    ),
    evaltype = ActionFilterEvalType.AndOr
  )

  protected[this] val operation00 = ActionOperationConfig(
    operationtype = OperationType.sendMessage,
    esc_period = Some(100),
    esc_step_from = Some(1),
    esc_step_to = Some(2),
    evaltype = Some(OperationEvalType.AndOr),
    message = Some(OpMessageConfig(
      default_msg = Some(true),
      mediaType = Some(testMediaTypes(0).description)
    )),
    opmessage_grp = Some(Seq(testUserGroups(0).userGroup.name))
  )

  protected[this] val operation01 = ActionOperationConfig(
    operationtype = OperationType.sendMessage,
    evaltype = Some(OperationEvalType.AndOr),
    message = Some(OpMessageConfig(
      default_msg = Some(true),
      mediaType = Some(testMediaTypes(1).description)
    )),
    opmessage_usr = Some(testUsers.map(_.user.alias))
  )

  protected[this] val operation10 = ActionOperationConfig(
    operationtype = OperationType.sendMessage,
    evaltype = Some(OperationEvalType.AndOr),
    message = Some(OpMessageConfig(
      default_msg = Some(false),
      mediaType = Some(testMediaTypes(0).description),
      message = Some("override message"),
      subject = Some("override subject")
    )),
    opmessage_usr = Some(testUsers.map(_.user.alias))
  )

  protected[this] val operation11 = ActionOperationConfig(
    operationtype = OperationType.sendMessage,
    message = Some(OpMessageConfig(
      default_msg = Some(true),
      mediaType = Some(testMediaTypes(1).description)
    )),
    opmessage_usr = Some(Seq(testUsers(0).user.alias))
  )

  protected[this] val operation20 = ActionOperationConfig(
    operationtype = OperationType.sendMessage,
    message = Some(OpMessageConfig(
      default_msg = Some(true),
      mediaType = Some(testMediaTypes(0).description)
    )),
    opmessage_usr = Some(Seq(testUsers(1).user.alias))
  )

  protected[this] val testActions: Seq[ActionConfig] = Seq(
    ActionConfig(
      specName("test action 0"),
      esc_period = 300,
      eventsource = EventSource.trigger,
      def_shortdata = Some("=== {TRIGGER.STATUS} === {HOST.NAME} - {TRIGGER.NAME}"),
      def_longdata = Some("this is long data"),
      filter = filter0,
      operations = Seq(operation00, operation01)
    ),
    ActionConfig(
      specName("test action 1"),
      esc_period = 400,
      eventsource = EventSource.trigger,
      def_shortdata = Some(""),
      def_longdata = Some("this is long data"),
      filter = filter1,
      operations = Seq(operation10, operation11)
    ),
    ActionConfig(
      specName("test action 2"),
      esc_period = 500,
      eventsource = EventSource.trigger,
      filter = ActionFilter.empty,
      operations = Seq(operation20)
    )
  )

  def presentTestActions(ops: Ops): Unit = {
    presentTestUsers(ops)
    await(ops.action.present(testActions))
  }

  def cleanTestActions(ops: Ops): Unit = {
    await(ops.action.absent(testActions.map(_.name)))
    cleanTestUsers(ops)
  }
}
