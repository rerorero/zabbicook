package com.github.zabbicook.test

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.host.HostGroup
import com.github.zabbicook.operation.HostGroupOp

import scala.concurrent.Future

trait TestHostGroups extends TestConfig { self: UnitSpec =>
  private[this] lazy val testHostGroupOp = new HostGroupOp(cachedApi)

  /**
    * you can override to customize.
    */
  protected[this] val testHostGroups: Seq[HostGroup[NotStored]] = Seq(
    HostGroup(name = specName("hostgroup1")),
    HostGroup(name = specName("hostgroup2"))
  )

  def presentTestHostGroups(): Unit = {
    await(Future.traverse(testHostGroups)(g => testHostGroupOp.present(g)))
  }

  def cleanTestHostGroups(): Unit = {
    await(testHostGroupOp.absent(testHostGroups.map(_.name)))
  }
}
