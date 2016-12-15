package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.screen.Screen
import com.github.zabbicook.test.{TestTemplateScreens, UnitSpec}

import scala.concurrent.Future

class TemplateScreenOpSpec extends UnitSpec with TestTemplateScreens {
  withTestOp { (ops, version) =>
    lazy val sut = ops.templateScreen

    version + "present screen" should "create, delete, and update global screens" in {
      val added = Screen[NotStored](
        name = specName("template screen X"),
        hsize = Some(3),
        vsize = Some(3)
      )

      def clean() = {
        await(sut.absent(testTemplates(0).template.host, Seq(added.name)))
        cleanTestTemplateScreens(ops)
      }

      cleanRun(clean) {
        def check(templateName: String, conf: Seq[Screen[NotStored]]): Unit = {
          val templateId = await(ops.template.findByHostnameAbsolutely(templateName)).template.getStoredId
          val actuals: Seq[Screen[Stored]] = await(Future.traverse(conf.map(_.name))(sut.findByName(templateId, _))).flatten
          assert(conf.length === actuals.length)
          conf.map { expected =>
            val Some(actual) = actuals.find(_.name == expected.name)
            assert(false === actual.shouldBeUpdated(expected))
          }
        }

        // creates
        {
          presentTestTemplateScreens(ops)
          check(testTemplates(0).template.host, testTemplateScreensFor1)
          check(testTemplates(1).template.host, testTemplateScreensFor2)
          // represent does nothing
          val report = await(sut.present(testTemplates(0).template.host, testTemplateScreensFor1))
          assert(report.isEmpty())
        }

        // append
        {
          val appended = testTemplateScreensFor1 :+ added
          val r = await(sut.present(testTemplates(0).template.host, appended))
          assert(1 === r.count)
          assert(added.entityName === r.created.head.entityName)
          check(testTemplates(0).template.host, appended)
          // represent does nothing
          val report2 = await(sut.present(testTemplates(0).template.host, appended))
          assert(report2.isEmpty())
        }

        // update
        {
          val updated = testTemplateScreensFor1 :+ added.copy(
            vsize = Some(11)
          )
          val r = await(sut.present(testTemplates(0).template.host, updated))
          assert(1 === r.count)
          assert(added.entityName === r.updated.head.entityName)
          check(testTemplates(0).template.host, updated)
          // represent does nothing
          val report2 = await(sut.present(testTemplates(0).template.host, updated))
          assert(report2.isEmpty())
        }
      }
    }
  }

}
