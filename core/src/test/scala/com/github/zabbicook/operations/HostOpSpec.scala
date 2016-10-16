package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.host._
import com.github.zabbicook.operation.StoredHost
import com.github.zabbicook.test.{TestHosts, UnitSpec}

class HostOpSpec extends UnitSpec with TestHosts {
  lazy val sut = testHostOp

  "present hosts" should "create, delete, and update hosts" in {
    val appended =
      HostConf(
        Host[NotStored](
          host = specName("test host X"),
          description = Some("test description X"),
          ipmi_authtype = Some(IpmiAuthAlgo.straight),
          ipmi_privilege = Some(IpmiPrivilege.admin),
          ipmi_username = Some("admin"),
          ipmi_password = Some("pass")
        ),
        Seq(testHostGroups(1).name),
        Seq(
          HostInterface[NotStored](
            ip = Some("127.0.0.9"),
            main = true,
            port = "10055",
            `type` = InterfaceType.agent,
            useip = InterfaceUseIp.ip
          ),
          HostInterface[NotStored](
            ip = Some("127.0.0.10"),
            main = true,
            port = "30000",
            `type` = InterfaceType.SNMP,
            useip = InterfaceUseIp.ip,
            bulk = Some(true)
          )
        ),
        Some(Seq(testTemplates.head.hostName))
      )

    def clean() = {
      await(sut.absent(Seq(appended.host.host)))
      cleanTestHosts()
    }

    cleanRun(clean) {
      assert(Seq() === await(sut.findByHostnames(testHosts.map(_.host.host))))

      def checkPresented(conf: Seq[HostConf], actuals: Seq[StoredHost]): Unit = {
        assert(conf.length === actuals.length)
        conf.map { expected =>
          val actual = actuals.find(_.host.host == expected.host.host).get
          assert(actual.host.shouldBeUpdated(expected.host) === false)
          assert(expected.hostGroups.toSet === actual.hostGroups.map(_.name).toSet)
          assert(expected.interfaces.length === actual.interfaces.length)
          expected.interfaces.foreach { ei =>
            val ai = actual.interfaces.find(_.isIdentical(ei)).get
            assert(ai.shouldBeUpdated(ei) === false)
          }
        }
      }

      // creates
      {
        presentTestHosts()
        val founds = await(sut.findByHostnames(testHosts.map(_.host.host)))
        checkPresented(testHosts, founds)
        // represent does nothing
        val report2 = await(sut.present(testHosts))
        assert(report2.isEmpty)
      }

      // appends
      {
        assert(None === await(sut.findByHostname(appended.host.host)))
        val added = testHosts :+ appended
        val report = await(sut.present(added))
        assert(report.count === 1)
        assert(appended.host.entityName === report.created.head.entityName)
        val founds = await(sut.findByHostnames(added.map(_.host.host)))
        checkPresented(added, founds)
        // represent does nothing
        val report2 = await(sut.present(added))
        assert(report2.isEmpty)
      }

      // update
      {
        def checkUpdated(expected: Seq[HostConf]): Unit = {
          val report = await(sut.present(expected))
          assert(report.count === 2) // delete and recreate
          assert(report.created.head.entityName === testHosts.head.host.entityName)
          assert(report.deleted.head.entityName === testHosts.head.host.entityName)
          val founds = await(sut.findByHostnames(expected.map(_.host.host)))
          checkPresented(expected, founds)
          // represent does nothing
          val report2 = await(sut.present(expected))
          assert(report2.isEmpty)
        }

        val hostModified = appended.copy(
          host = appended.host.copy(
            description = Some("updated description Y"),
            ipmi_authtype = Some(IpmiAuthAlgo.default),
            ipmi_privilege = Some(IpmiPrivilege.operator),
            ipmi_username = Some("guest"),
            ipmi_password = Some("0000")
          )
        )
        checkUpdated(testHosts :+ hostModified)

        val groupModified = hostModified.copy(
          hostGroups = testHostGroups.map(_.name)
        )
        checkUpdated(testHosts :+ groupModified)

        val if1 = groupModified.interfaces(1)
        val ifModified = groupModified.copy(
          interfaces = groupModified.interfaces.updated(1, if1.copy(bulk = Some(false)))
        )
        checkUpdated(testHosts :+ ifModified)

        val templateModified = ifModified.copy(
          templates = Some(Seq(testTemplates(1).hostName))
        )
        checkUpdated(testHosts :+ templateModified)
      }
    }
  }
}
