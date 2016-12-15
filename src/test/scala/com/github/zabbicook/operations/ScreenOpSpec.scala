package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.screen.Screen
import com.github.zabbicook.test.{TestScreens, UnitSpec}

import scala.concurrent.Future

class ScreenOpSpec extends UnitSpec with TestScreens {
  withTestOp { (ops, version) =>
    lazy val sut = ops.screen

    version + "present screen" should "create, delete, and update global screens" in {
      val added = Screen[NotStored](
        name = specName("screen X"),
        hsize = Some(3),
        vsize = Some(3)
      )

      def clean() = {
        await(sut.absent(Seq(added.name)))
        cleanTestScreens(ops)
      }

      cleanRun(clean) {
        def check(conf: Seq[Screen[NotStored]]): Unit = {
          val actuals: Seq[Screen[Stored]] = await(Future.traverse(conf.map(_.name))(sut.findByName)).flatten
          assert(conf.length === actuals.length)
          conf.map { expected =>
            val Some(actual) = actuals.find(_.name == expected.name)
            assert(false === actual.shouldBeUpdated(expected))
          }
        }

        // creates
        {
          presentTestScreens(ops)
          check(testScreens)
          // represent does nothing
          val report = await(sut.present(testScreens))
          assert(report.isEmpty())
        }

        // append
        {
          val appended = testScreens :+ added
          val r = await(sut.present(appended))
          assert(1 === r.count)
          assert(added.entityName === r.created.head.entityName)
          check(appended)
          // represent does nothing
          val report2 = await(sut.present(appended))
          assert(report2.isEmpty())
        }

        // update
        {
          val updated = testScreens :+ added.copy(
            hsize = Some(11)
          )
          val r = await(sut.present(updated))
          assert(1 === r.count)
          assert(added.entityName === r.updated.head.entityName)
          check(updated)
          // represent does nothing
          val report2 = await(sut.present(updated))
          assert(report2.isEmpty())
        }
      }
    }
  }
}
