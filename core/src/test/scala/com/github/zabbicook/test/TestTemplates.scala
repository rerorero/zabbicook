package com.github.zabbicook.test

import com.github.zabbicook.entity.template.{Template, TemplateSettings}
import com.github.zabbicook.operation.Ops

trait TestTemplates extends TestHostGroups { self: UnitSpec =>

  /**
    * you can override to customize generated users.
    */
  protected[this] val testTemplates: Seq[TemplateSettings.NotStoredAll] = Seq(
    TemplateSettings(Template(host = specName("template1")), Seq(testHostGroups(0)), None),
    TemplateSettings(Template(host = specName("template2")), Seq(testHostGroups(1)),
      Some(Seq(Template(host = specName("template1")), Template(host = "Template OS Linux")))),
    TemplateSettings(Template(host = specName("template3")), Seq(testHostGroups(0)), None)
  )

  def presentTestTemplates(ops: Ops): Unit = {
    presentTestHostGroups(ops)
    await(ops.template.present(testTemplates))
  }

  def cleanTestTemplates(ops: Ops): Unit = {
    await(ops.template.absent(testTemplates.map(_.hostName)))
    cleanTestHostGroups(ops)
  }
}
