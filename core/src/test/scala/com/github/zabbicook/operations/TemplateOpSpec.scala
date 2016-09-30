package com.github.zabbicook.operations

import com.github.zabbicook.entity.Template
import com.github.zabbicook.operation.{TemplateOp, TemplateSettings}
import com.github.zabbicook.test.{TestTemplates, UnitSpec}

class TemplateOpSpec extends UnitSpec with TestTemplates {
  lazy val sut = new TemplateOp(cachedApi)

  "presentTemplates and absentTemplates" should "create, delete and update templates" in {
    val appended: TemplateSettings.NotStoredAll = TemplateSettings(
      Template(host = specName("templateX")),
      Seq(testHostGroups(1)),
      Some(Seq(testTemplates(1).template)))

    def clean() = {
      await(sut.absentTemplates(Seq(appended.template.host)))
      cleanTestTemplates()
    }

    cleanRun(clean) {
      assert(Seq() === await(sut.findByHostnames(testTemplates.map(_.template.host))))

      // creates
      {
        presentTestTemplates()
        val founds = await(sut.findByHostnames(testTemplates.map(_.template.host)))
        assert(founds.length === testTemplates.length)
        testTemplates.map { expected =>
          val actual = founds.find(expected.template.host == _.template.host).get
          assert(actual.template.shouldBeUpdated(expected.template) === false)
          assert(expected.groupsNames === actual.groupsNames)
          assert(expected.linkedTemplateHostNames === actual.linkedTemplateHostNames)
        }
      }

      // appends
      {
        val (_, report) = await(sut.presentTemplates(testTemplates :+ appended))
        assert(report.count === 1)
        assert(report.created.head.entityName === appended.template.entityName)
        val founds = await(sut.findByHostnames((testTemplates :+ appended).map(_.hostName)))
        assert(founds.length === testTemplates.length + 1)
        // represent does nothing
        val (_, report2) = await(sut.presentTemplates(testTemplates :+ appended))
        assert(report2.isEmpty)
      }

      // update
      {
        val modified: TemplateSettings.NotStoredAll = TemplateSettings(
          Template(host = specName("templateX")),
          Seq(testHostGroups(0)),
          Some(Seq(testTemplates(0).template)))

        val (_, report) = await(sut.presentTemplates(testTemplates :+ modified))
        assert(report.count === 1)
        assert(report.updated.head.entityName === modified.template.entityName)
        val Some(actual) = await(sut.findByHostname(modified.template.host))
        assert(modified.hostName === actual.hostName)
        assert(modified.groupsNames === actual.groupsNames)
        assert(modified.linkedTemplateHostNames === actual.linkedTemplateHostNames)
      }

      // absent
      {
        val (_, report) = await(sut.absentTemplates((testTemplates :+ appended).map(_.hostName)))
        assert(report.count === testTemplates.length + 1)
        report.deleted.take(report.count).foreach(e => assert(e.entityName === appended.template.entityName))
        assert(Seq() === await(sut.findByHostnames(testTemplates.map(_.hostName))))
        // reabsent does nothing
        val (_, report2) = await(sut.absentTemplates((testTemplates :+ appended).map(_.hostName)))
        assert(report2.isEmpty())
      }
    }
  }
}
