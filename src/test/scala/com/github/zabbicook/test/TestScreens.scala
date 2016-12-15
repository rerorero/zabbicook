package com.github.zabbicook.test

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.screen.Screen
import com.github.zabbicook.operation.Ops

trait TestScreens extends TestConfig { self: UnitSpec =>

  protected[this] val testScreens: Seq[Screen[NotStored]] = Seq(
    Screen(name = specName("screen0"), hsize = Some(8), vsize = Some(8)),
    Screen(name = specName("screen1"), hsize = Some(5), vsize = Some(5)),
    Screen(name = specName("screen2"), hsize = Some(10), vsize = Some(10))
  )

  def presentTestScreens(ops: Ops): Unit = {
    await(ops.screen.present(testScreens))
  }

  def cleanTestScreens(ops: Ops): Unit = {
    await(ops.screen.absent(testScreens.map(_.name)))
  }
}
