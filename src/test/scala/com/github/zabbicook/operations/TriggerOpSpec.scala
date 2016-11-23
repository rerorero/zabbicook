package com.github.zabbicook.operations

import com.github.zabbicook.api.Version
import com.github.zabbicook.entity.trigger._
import com.github.zabbicook.test.{TestTriggers, UnitSpec}

class TriggerOpSpec extends UnitSpec with TestTriggers {
  withTestOp { (ops, version) =>
    val sut = ops.trigger

    version + "present" should "create, delete and update items in templates" in {
      val _appendedConf = TriggerConf(
        Trigger(
          description = specName(s" item1 on {HOST.NAME}"),
          expression = s"{${testTemplates(0).template.host}:${item1.`key_`}.diff(0)}=0",
          comments = Some("appended"),
          priority = Some(Severity.average)
        )
      )
      val appendedConf = if (version >= Version.of("3.2.0")) {
        _appendedConf.copy(
          tags = Some(Seq(
            TriggerTag("tag1", "abc"),
            TriggerTag("tag2", "two2")
          )),
          dependencies = Some(Seq(
            TriggerDependenciesConf(None, trigger0.trigger.description)
          ))
        )
      } else {
        _appendedConf
      }

      val appended = testTemplates(0).template.host -> appendedConf

      def clean() = {
        cleanTestTriggers(ops)
      }

      def check(constants: Map[String, Seq[TriggerConf]]): Unit = {
        constants.foreach { case (templateName, configs) =>
          val Some(template) = await(ops.template.findByHostname(templateName))
          val founds = await(sut.getBelongingTriggers(template.template.getStoredId))
          assert(configs.length === founds.length)
          (configs.sortBy(_.trigger.description) zip founds.sortBy(_.trigger.description)) foreach { case (expectedConf, actual) =>
            val expected = await(sut.configToNotStored(template.template, expectedConf))
            assert(false === actual.shouldBeUpdated(expected))
          }
        }
      }

      cleanRun(clean) {

        // creates
        {
          presentTestTriggers(ops)
          check(testTriggers)
        }

        // append
        {
          val updated = testTriggers.updated(appended._1, testTriggers(appended._1) :+ appended._2)
          val report = await(sut.presentWithTemplate(updated))
          assert(1 === report.count)
          assert(appended._2.trigger.entityName === report.created.head.entityName)
          check(updated)
          // represent
          val report2 = await(sut.presentWithTemplate(updated))
          assert(report2.isEmpty())
        }

        // update
        {
          val updated = testTriggers.updated(appended._1, testTriggers(appended._1) :+ appended._2.copy(trigger = appended._2.trigger.copy(
            expression = s"{${testTemplates(0).template.host}:${item1.`key_`}.diff(0)}>0",
            comments = Some("modified"),
            priority = Some(Severity.high)
          )))
          val report = await(sut.presentWithTemplate(updated))
          assert(1 === report.count)
          assert(appended._2.trigger.entityName === report.updated.head.entityName)
          check(updated)
          // represent
          val report2 = await(sut.presentWithTemplate(updated))
          assert(report2.isEmpty())
        }

        // absent
        {
          val report = await(sut.presentWithTemplate(testTriggers))
          assert(1 === report.count)
          assert(appended._2.trigger.entityName === report.deleted.head.entityName)
          check(testTriggers)
          // represent
          val report2 = await(sut.presentWithTemplate(testTriggers))
          assert(report2.isEmpty())
        }
      }
    }

    version + "present" should "update overloaded triggers" in {
      val childTemplate = testTemplates.find(_.linkedTemplateHostNames.getOrElse(Set()).contains("Template OS Linux")).head

      val description = "Too many processes on {HOST.NAME}"
      val expression = s"{${childTemplate.template.host}:proc.num[].avg(5m)}>300"
      val appended = childTemplate.template.host -> TriggerConf(
          Trigger(
            description = description,
            expression = expression,
            status = Some(false)
          )
        )

      def clean() = {
        cleanTestTriggers(ops)
      }

      def check(constants: Map[String, Seq[TriggerConf]]): Unit = {
        constants.foreach { case (templateName, configs) =>
          val Some(template) = await(ops.template.findByHostname(templateName))
          val founds = await(sut.getBelongingTriggers(template.template.getStoredId))
          assert(configs.length === founds.length)
          (configs.sortBy(_.trigger.description) zip founds.sortBy(_.trigger.description)) foreach { case (expectedConf, actual) =>
            val expected = await(sut.configToNotStored(template.template, expectedConf))
            assert(false === actual.shouldBeUpdated(expected))
          }
        }
      }

      def checkOverrided(): Unit = {
        val Seq(t) = await(ops.template.findByHostnamesAbsolutely(Seq(childTemplate.template.host)))
        val inherits = await(sut.getInheritedTriggers(t.template.getStoredId))
        val actual = inherits.find(_.trigger.description == description).get
        assert(actual.trigger.expression === expression)
      }

      cleanRun(clean) {
        // creates
        {
          presentTestTriggers(ops)
          check(testTriggers)
        }

        // override
        {
          val updated = testTriggers.updated(appended._1, testTriggers(appended._1) :+ appended._2)
          val report = await(sut.presentWithTemplate(updated))
          assert(1 === report.count)
          assert(appended._2.trigger.entityName === report.updated.head.entityName)
          check(testTriggers)
          checkOverrided()
          // represent
          val report2 = await(sut.presentWithTemplate(updated))
          assert(report2.isEmpty())
        }

        // absent
        {
          val report = await(sut.presentWithTemplate(testTriggers))
          assert(report.isEmpty())
          check(testTriggers)
          checkOverrided()
        }
      }
    }
  }
}
