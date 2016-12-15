package com.github.zabbicook.entity.screen

import com.github.zabbicook.entity.prop.IntProp
import com.github.zabbicook.entity.screen.ScreenResourceType.systemStatus
import com.github.zabbicook.hocon.{HoconReader, HoconSuccess}
import com.github.zabbicook.test.UnitSpec
import com.github.zabbicook.hocon.HoconReadsCompanion._
import com.github.zabbicook.hocon.HoconReads.option

class ScreenItemSettingSpec extends UnitSpec {

  "metahelper" should "parse config" in {
    val conf =
      """
        |{
        |  resourceType: systemStatus
        |  x: 2
        |  y: 4
        |}
      """.stripMargin
    val HoconSuccess(actual) = HoconReader.read[ScreenItemSetting](conf, ScreenItemSetting.optional("root"))
    assert(systemStatus === actual.resourcetype)
    assert(Some(IntProp(2)) === actual.x)
    assert(Some(IntProp(4)) === actual.y)
  }
}
