package com.github.zabbicook.test

import com.github.zabbicook.entity.HostGroup
import com.github.zabbicook.entity.HostGroup.HostGroupId
import com.github.zabbicook.operation.HostGroupOp

import scala.concurrent.Future

trait TestHostGroups extends TestConfig { self: UnitSpec =>
  private[this] lazy val testHostGroupOp = new HostGroupOp(cachedApi)

  /**
    * you can override to customize.
    */
  protected[this] val testHostGroups = Seq(
    HostGroup(name = specName("hostgroup1")),
    HostGroup(name = specName("hostgroup2"))
  )

  def presentTestHostGroups(): Seq[HostGroupId] = {
    await(Future.traverse(testHostGroups) (g => testHostGroupOp.create(g)).map(_.map(_._1)))
  }

  def cleanTestHostGroups(): Unit = {
    await(testHostGroupOp.absent(testHostGroups.map(_.name)))
  }
}
