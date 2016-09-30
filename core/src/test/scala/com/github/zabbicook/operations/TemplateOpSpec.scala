package com.github.zabbicook.operations

import com.github.zabbicook.entity.Template
import com.github.zabbicook.operation.{TemplateOp, TemplateSettings}
import com.github.zabbicook.test.{TestTemplates, UnitSpec}

class TemplateOpSpec extends UnitSpec with TestTemplates {
  lazy val sut = new TemplateOp(cachedApi)

  "presentTemplates and absentTemplates" should "create, delete and update templates" in {
    val appended = TemplateSettings(Template(host = specName("templateX")), Seq(testHostGroups(1)),
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
          assert(expected.groups.map(_.name).toSet === actual.groups.map(_.name).toSet)
          assert(expected.linkedTemplates.map(_.map(_.host).toSet) === actual.linkedTemplates.map(_.map(_.host).toSet))
        }
      }

      // appends
      {
      }
    }
  }
}
