package com.github.zabbicook.operations

import com.github.zabbicook.entity.User
import com.github.zabbicook.hocon.HoconSuccess
import com.github.zabbicook.operation.UserConfig
import com.github.zabbicook.test.UnitSpec
import com.typesafe.config.ConfigFactory

class UserConfigSpec extends UnitSpec {
  "UserConfig" should "parsed from Hocon" in {
    val s = s"""{
       |  alias: "Alice"
       |  lang: "en"
       |  groups: ["g1", "g2"]
       |  password: "pass"
       |}""".stripMargin
    val actual = UserConfig.hoconReads.read(ConfigFactory.parseString(s))
    assert(actual === HoconSuccess(UserConfig(
      user = User(alias = "Alice", lang = Some("en")),
      groupNames = Set("g1", "g2"),
      password = "pass"
    )))
  }
}
