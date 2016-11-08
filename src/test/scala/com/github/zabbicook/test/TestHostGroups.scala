package com.github.zabbicook.test

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.host.HostGroup
import com.github.zabbicook.operation.Ops

import scala.concurrent.Future

trait TestHostGroups extends TestConfig { self: UnitSpec =>
  /**
    * you can override to customize.
    */
  protected[this] val testHostGroups: Seq[HostGroup[NotStored]] = Seq(
    HostGroup(name = specName("hostgroup1")),
    HostGroup(name = specName("hostgroup2")),
    HostGroup(name = specName("hostgroup3"))
  )

  def presentTestHostGroups(ops: Ops): Unit = {
    await(Future.traverse(testHostGroups)(g => ops.hostGroup.present(g)))
  }

  def cleanTestHostGroups(ops: Ops): Unit = {
    await(ops.hostGroup.absent(testHostGroups.map(_.name)))
  }
}
