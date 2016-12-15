package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.Stored
import com.github.zabbicook.entity.screen._
import com.github.zabbicook.test.{TestScreenItems, UnitSpec}

class ScreenItemOpSpec extends UnitSpec with TestScreenItems {
  withTestOp { (ops, version) =>
    lazy val sut = ops.screenItem

    version + "present screen" should "create, delete, and update global screens" in {
      val added = ScreenItemSetting(
        resourcetype = ScreenResourceType.clock,
        halign = Some(HAlign.right),
        height = Some(250),
        width = Some(250),
        valign = Some(VAlign.top),
        x = Some(5),
        y = Some(5)
      )

      def clean() = {
        cleanTestScreenItems(ops)
      }

      cleanRun(clean) {
        def check(screenName: String, conf: Seq[ScreenItemSetting]): Unit = {
          val screenId = await(ops.screen.findByNameAbsolutely(screenName)).getStoredId
          val actuals: Seq[ScreenItem[Stored]] = await(sut.findByScreenId(None, screenId))
          assert(conf.length === actuals.length)
          conf.map { expected =>
            val Some(actual) = actuals.find(_.key == expected.key)
            val resolvedExpect = await(sut.resolveResource(screenId, None, expected))
            assert(false === actual.shouldBeUpdated(resolvedExpect))
          }
        }

        // creates
        {
          presentTestScreenItems(ops)
          testScreenItems.foreach(kv => check(kv._1, kv._2))
          // represent does nothing
          testScreenItems.foreach { kv =>
            val report = await(sut.present(kv._1, kv._2, None))
            assert(report.isEmpty())
          }
        }

        // append
        {
          val appended = testScreenItemsFor0 :+ added
          await(sut.present(testScreens(0).name, appended, None))
          check(testScreens(0).name, appended)
          // represent does nothing
          val report2 = await(sut.present(testScreens(0).name, appended, None))
          assert(report2.isEmpty())
        }

        // update
        {
          val updated = testScreenItemsFor0 :+ added.copy(
            height = Some(100),
            width = Some(500)
          )
          await(sut.present(testScreens(0).name, updated, None))
          check(testScreens(0).name, updated)
          // represent does nothing
          val report2 = await(sut.present(testScreens(0).name, updated, None))
          assert(report2.isEmpty())
        }
      }
    }
  }
}
