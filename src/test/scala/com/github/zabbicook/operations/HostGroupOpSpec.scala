package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.host.HostGroup
import com.github.zabbicook.test.{TestHostGroups, UnitSpec}

class HostGroupOpSpec extends UnitSpec with TestHostGroups {
  withTestOp { (ops, version) =>
    lazy val sut = ops.hostGroup

    version + "present and absent" should "create and delete Host Groups" in {
      val appended = HostGroup[NotStored](name = specName("hostgroupx"))

      def clean(): Unit = {
        cleanTestHostGroups(ops)
        await(sut.absent(Seq(appended.name)))
      }

      cleanRun(clean) {
        assert(Seq() === await(sut.findByNames(testHostGroups.map(_.name))))

        // creates
        {
          presentTestHostGroups(ops)
          val founds = await(sut.findByNames(testHostGroups.map(_.name)))
          assert(founds.length === testHostGroups.length)
          assert(testHostGroups.map(_.name).toSet === founds.map(_.name).toSet)
        }

        // appends
        {
          val report = await(sut.present(testHostGroups :+ appended))
          assert(report.count === 1)
          assert(report.created.head.entityName === appended.entityName)
          val founds = await(sut.findByNames((testHostGroups :+ appended).map(_.name)))
          assert(founds.length === testHostGroups.length + 1)
          // represent does nothing
          val report2 = await(sut.present(testHostGroups :+ appended))
          assert(report2.isEmpty)
        }

        // absent
        {
          val report = await(sut.absent((testHostGroups :+ appended).map(_.name)))
          assert(report.count === testHostGroups.length + 1)
          report.deleted.take(report.count).foreach(e => assert(e.entityName === appended.entityName))
          assert(Seq() === await(sut.findByNames(testHostGroups.map(_.name))))
          // reabsent does nothing
          val report2 = await(sut.absent((testHostGroups :+ appended).map(_.name)))
          assert(report2.isEmpty())
        }
      }
    }
  }
}
