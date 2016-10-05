package com.github.zabbicook.test

import com.github.zabbicook.entity.Template
import com.github.zabbicook.operation.{TemplateOp, TemplateSettings}

trait TestTemplates extends TestHostGroups { self: UnitSpec =>

  private[this] lazy val templateOp = new TemplateOp(cachedApi)

  /**
    * you can override to customize generated users.
    */
  protected[this] val testTemplates: Seq[TemplateSettings.NotStoredAll] = Seq(
    TemplateSettings(Template(host = specName("template1")), Seq(testHostGroups(0)), None),
    TemplateSettings(Template(host = specName("template2")), Seq(testHostGroups(1)),
      Some(Seq(Template(host = specName("template1")), Template(host = "Template OS Linux"))))
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
