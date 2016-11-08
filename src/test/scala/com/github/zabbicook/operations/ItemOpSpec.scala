package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.item.{Item, ItemType, ValueType}
import com.github.zabbicook.test.{TestItems, UnitSpec}

class ItemOpSpec extends UnitSpec with TestItems {
  withTestOp { (ops, version) =>

    val sut = ops.item

    version + "present" should "create, delete and update items in templates" in {
      val appended: (String, Item[NotStored]) = (
        testTemplates(1).template.host,
        Item(
          delay = 200,
          `key_` = "net.udp.service[ntp]",
          name = specName("item appended - ntp"),
          `type` = ItemType.SimpleCheck,
          value_type = ValueType.unsigned,
          history = Some(10),
          port = Some(10099)
        )
        )

      def clean() = {
        cleanTestItems(ops)
      }

      cleanRun(clean) {
        assert(None === await(ops.template.findByHostname(appended._1)))

        // creates
        {
          presentTestItems(ops)
          testItems.foreach { case (hostname, items) =>
            val Some(template) = await(ops.template.findByHostname(hostname))
            val founds = await(sut.getBelongingItems(template.template.getStoredId))
            assert(founds.length === items.length)
            items.foreach { expected =>
              val actual = founds.find(expected.`key_` == _.`key_`).get
              assert(actual.shouldBeUpdated(expected) === false)
              assert(expected.name === actual.name)
              assert(expected.`type` === actual.`type`)
            }
          }
        }

        val Some(ts) = await(ops.template.findByHostname(appended._1))
        val hostid = ts.template.getStoredId

        // appends
        {
          val updates = testItems.updated(appended._1, testItems(appended._1) :+ appended._2)
          val report = await(sut.presentWithTemplate(updates))
          assert(report.count === 1)
          assert(report.created.head.entityName === appended._2.entityName)
          val founds = await(sut.getBelongingItems(hostid))
          assert(founds.length === testItems(appended._1).length + 1)
          // represent does nothing
          val report2 = await(sut.presentWithTemplate(updates))
          assert(report2.isEmpty)
        }

        // update
        {
          val modified: (String, Item[NotStored]) = appended.copy(_2 =
            Item(
              delay = 1000,
              `key_` = "net.udp.service[ntp]",
              name = specName("item appended - ntp new!"),
              `type` = ItemType.SimpleCheck,
              value_type = ValueType.unsigned,
              history = Some(20)
            )
          )

          val updates = testItems.updated(modified._1, testItems(modified._1) :+ modified._2)
          val report = await(sut.presentWithTemplate(updates))
          assert(report.count === 1)
          assert(report.updated.head.entityName === appended._2.entityName)
          val founds = await(sut.getBelongingItems(hostid))
          assert(founds.length === testItems(modified._1).length + 1)
          val Some(actual) = founds.find(_.`key_` == modified._2.`key_`)
          assert(actual.shouldBeUpdated(modified._2) === false)
          assert(modified._2.name === actual.name)
          assert(modified._2.delay === actual.delay)
          assert(modified._2.history === actual.history)
        }

        // absent
        {
          val report = await(sut.presentWithTemplate(testItems))
          assert(report.count === 1)
          assert(report.deleted.head.entityName === appended._2.entityName)
          val founds = await(sut.getBelongingItems(hostid))
          assert(founds.length === testItems(appended._1).length)
          assert(None === founds.find(_.`key_` == appended._2.`key_`))
          // reabsent does nothing
          val report2 = await(sut.presentWithTemplate(testItems))
          assert(report2.isEmpty())
        }
      }
    }
  }
}
