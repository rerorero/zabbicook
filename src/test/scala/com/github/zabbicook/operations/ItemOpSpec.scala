package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.item.{Item, ItemType, ValueType}
import com.github.zabbicook.operation.Report
import com.github.zabbicook.test.{TestItems, UnitSpec}

class ItemOpSpec extends UnitSpec with TestItems {
  withTestOp { (ops, version) =>

    val sut = ops.item

    version + "present" should "create, delete and update items in templates" in {
      val appended: Item[NotStored] = Item(
          delay = 200,
          `key_` = "net.udp.service[ntp]",
          name = specName("item appended - ntp"),
          `type` = ItemType.SimpleCheck,
          value_type = ValueType.unsigned,
          history = Some(10),
          port = Some(10099)
      )

      def clean() = {
        cleanTestItems(ops)
      }

      def present(settings: Seq[TestItemsSetting]): Report = {
        presentItems(sut, settings)
      }

      cleanRun(clean) {
        // creates
        {
          presentTestItems(ops)
          testItems.foreach { case TestItemsSetting(hostname, apps, items) =>
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

        val Some(ts) = await(ops.template.findByHostname(testItems(0).template))
        val hostid = ts.template.getStoredId

        // appends
        {
          val appendedItems = testItems(0).items :+ appended
          val updates = testItems.updated(0, testItems(0).copy(items = appendedItems))
          val report = present(updates)
          assert(report.count === 1)
          assert(report.created.head.entityName === appended.entityName)
          val founds = await(sut.getBelongingItems(hostid))
          assert(founds.length === testItems(0).items.length + 1)
          // represent does nothing
          val report2 = present(updates)
          assert(report2.isEmpty)
        }

        // update
        {
          val modified: Item[NotStored] = appended.copy(
            delay = 1000,
            `key_` = "net.udp.service[ntp]",
            name = specName("item appended - ntp new!"),
            `type` = ItemType.SimpleCheck,
            value_type = ValueType.unsigned,
            history = Some(20)
          )

          val appendedItems = testItems(0).items :+ modified
          val updates = testItems.updated(0, testItems(0).copy(items = appendedItems))
          val report = present(updates)
          assert(report.count === 1)
          assert(report.updated.head.entityName === modified.entityName)
          val founds = await(sut.getBelongingItems(hostid))
          assert(founds.length === testItems(0).items.length + 1)
          val Some(actual) = founds.find(_.`key_` == modified.`key_`)
          assert(actual.shouldBeUpdated(modified) === false)
          assert(modified.name === actual.name)
          assert(modified.delay === actual.delay)
          assert(modified.history === actual.history)
        }

        // absent
        {
          val report = present(testItems)
          assert(report.count === 1)
          assert(report.deleted.head.entityName === appended.entityName)
          val founds = await(sut.getBelongingItems(hostid))
          assert(founds.length === testItems(0).items.length)
          assert(None === founds.find(_.`key_` == appended.`key_`))
          // reabsent does nothing
          val report2 = present(testItems)
          assert(report2.isEmpty())
        }
      }
    }
  }
}
