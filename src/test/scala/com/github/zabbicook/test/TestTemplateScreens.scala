package com.github.zabbicook.test

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.screen.Screen
import com.github.zabbicook.operation.Ops

trait TestTemplateScreens extends TestTemplates { self: UnitSpec =>

  protected[this] val testTemplateScreensFor1: Seq[Screen[NotStored]] = Seq(
    Screen(name = specName("screen1-0"), hsize = Some(1), vsize = Some(2)),
    Screen(name = specName("screen1-1"), hsize = Some(5), vsize = Some(1)),
    Screen(name = specName("screen1-2"), hsize = Some(10), vsize = Some(10))
  )
  protected[this] val testTemplateScreensFor2: Seq[Screen[NotStored]] = Seq(
    Screen(name = specName("screen2-0"), hsize = Some(5), vsize = Some(1)),
    Screen(name = specName("screen2-1"), hsize = Some(2), vsize = Some(9))
  )

  def presentTestTemplateScreens(ops: Ops): Unit = {
    presentTestTemplates(ops)
    await(ops.templateScreen.present(testTemplates(0).template.host, testTemplateScreensFor1))
    await(ops.templateScreen.present(testTemplates(1).template.host, testTemplateScreensFor2))
  }

  def cleanTestTemplateScreens(ops: Ops): Unit = {
    await(ops.templateScreen.absent(testTemplates(0).template.host, testTemplateScreensFor1.map(_.name)))
    await(ops.templateScreen.absent(testTemplates(1).template.host, testTemplateScreensFor2.map(_.name)))
    cleanTestTemplates(ops)
  }
}
