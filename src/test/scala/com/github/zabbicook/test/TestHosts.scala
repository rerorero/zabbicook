package com.github.zabbicook.test

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.host._
import com.github.zabbicook.operation.Ops

trait TestHosts extends TestTemplates { self: UnitSpec =>
  /**
    * you can override to customize.
    */
  protected[this] val testHosts: Seq[HostConf] = Seq(
    HostConf(
      Host[NotStored](
        host = specName("test host1"),
        description = Some("test description1"),
        inventory_mode = Some(InventoryMode.automatic),
        ipmi_authtype = Some(IpmiAuthAlgo.MD5),
        ipmi_privilege = Some(IpmiPrivilege.operator),
        ipmi_username = Some("foo"),
        ipmi_password = Some("bar")
      ),
      Seq(testHostGroups(0).name),
      Seq(
        HostInterface[NotStored](
          ip = Some("127.0.0.1"),
          main = true,
          port = "10055",
          `type` = InterfaceType.agent,
          useip = InterfaceUseIp.ip
        ),
        HostInterface[NotStored](
          ip = Some("127.0.0.1"),
          main = false,
          port = "10099",
          `type` = InterfaceType.agent,
          useip = InterfaceUseIp.ip
        ),
        HostInterface[NotStored](
          dns = Some("example.com"),
          main = true,
          port = "161",
          `type` = InterfaceType.SNMP,
          useip = InterfaceUseIp.dns
        )
      ),
      Some(Seq(testTemplates(0).template.host))
    ),
    HostConf(
      Host[NotStored](
        host = specName("test host2"),
        status = Some(false)
      ),
      testHostGroups.map(_.name),
      Seq(
        HostInterface[NotStored](
          ip = Some("127.0.0.2"),
          main = true,
          port = "10059",
          `type` = InterfaceType.agent,
          useip = InterfaceUseIp.ip
        )
      ),
      Some(Seq(testTemplates(1).hostName, testTemplates(2).hostName))
    )
  )

  def presentTestHosts(ops: Ops): Unit = {
    presentTestTemplates(ops)
    await(ops.host.present(testHosts))
  }

  def cleanTestHosts(ops: Ops): Unit = {
    await(ops.host.absent(testHosts.map(_.host.host)))
    cleanTestTemplates(ops)
  }
}
