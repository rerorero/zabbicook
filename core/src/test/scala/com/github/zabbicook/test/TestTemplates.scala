package com.github.zabbicook.test

import com.github.zabbicook.entity.template.{Template, TemplateSettings}
import com.github.zabbicook.operation.{HostGroupOp, TemplateOp}

trait TestTemplates extends TestHostGroups { self: UnitSpec =>

  protected[this] lazy val templateOp = new TemplateOp(cachedApi, new HostGroupOp(cachedApi))

  /**
    * you can override to customize generated users.
    */
  protected[this] val testTemplates: Seq[TemplateSettings.NotStoredAll] = Seq(
    TemplateSettings(Template(host = specName("template1")), Seq(testHostGroups(0)), None),
    TemplateSettings(Template(host = specName("template2")), Seq(testHostGroups(1)),
      Some(Seq(Template(host = specName("template1")), Template(host = "Template OS Linux")))),
    TemplateSettings(Template(host = specName("template3")), Seq(testHostGroups(0)), None)
  )

  def presentTestTemplates(): Unit = {
    presentTestHostGroups()
    await(templateOp.present(testTemplates))
  }

  def cleanTestTemplates(): Unit = {
    await(templateOp.absent(testTemplates.map(_.hostName)))
    cleanTestHostGroups()
  }
}
