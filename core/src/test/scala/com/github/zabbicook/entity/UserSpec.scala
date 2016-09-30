package com.github.zabbicook.entity

import com.github.zabbicook.hocon.{HoconReader, HoconSuccess}
import com.github.zabbicook.test.UnitSpec

class UserSpec extends UnitSpec {

  "User" should "be parsed in Hocon format" in {
    val s =
      s"""{
         |alias: "Alice"
         |autoLogin: true
         |theme: "dark"
         |type: "user"
         |}""".stripMargin
    val HoconSuccess(actual) = HoconReader.read[User](s)
    assert(User(
      alias = "Alice",
      autologin = Some(EnabledEnum.enabled),
      theme = Some(Theme.dark),
      `type` = Some(UserType.user)
    ) === actual)
  }
}
