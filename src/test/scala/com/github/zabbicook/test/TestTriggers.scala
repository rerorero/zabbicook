package com.github.zabbicook.test

import com.github.zabbicook.entity.trigger.{Severity, Trigger, TriggerConf}
import com.github.zabbicook.operation.Ops

trait TestTriggers extends TestItems { self: UnitSpec =>

  protected[this] val trigger0 = TriggerConf(
    Trigger(
      description = specName(s" item0 on {HOST.NAME}"),
      expression = s"{${testTemplates(0).template.host}:${item0.`key_`}.diff(0)}>0",
      comments = Some("Trigger"),
      priority = Some(Severity.high),
      url = Some("http://example.com")
    )
  )

  protected[this] val trigger1 = TriggerConf(
    Trigger(
      description = specName(s" item0 on {HOST.NAME} 2"),
      expression = s"{${testTemplates(0).template.host}:${item0.`key_`}.diff(0)}=0",
      status = Some(false)
    )
  )

  protected[this] val trigger2 = TriggerConf(
    Trigger(
      description = specName(s" item2 on {HOST.NAME}"),
      expression = s"{${testTemplates(1).template.host}:${item2.`key_`}.change(0)}<0",
      priority = Some(Severity.information)
    )
  )

  protected[this] val testTriggers: Map[String, Seq[TriggerConf]] = Map(
    testTemplates(0).template.host -> Seq(trigger0, trigger1),
    testTemplates(1).template.host -> Seq(trigger2)
  )

  def presentTestTriggers(ops: Ops): Unit = {
    presentTestItems(ops)
    await(ops.trigger.presentWithTemplate(testTriggers))
  }

  def cleanTestTriggers(ops: Ops): Unit = {
    await(ops.trigger.absentWithTemplate(testTriggers.mapValues(_.map(_.trigger.description))))
    cleanTestItems(ops)
  }
}
