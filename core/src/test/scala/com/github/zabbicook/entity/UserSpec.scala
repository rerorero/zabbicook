package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.prop.EnabledEnum
import com.github.zabbicook.hocon._
import com.github.zabbicook.test.UnitSpec

class UserSpec extends UnitSpec {

  import com.github.zabbicook.hocon.HoconReadsCompanion._

  import com.github.zabbicook.hocon.HoconReads2.option

  "User" should "be parsed in Hocon format" in {
    val s =
      s"""{
         |alias: "Alice"
         |autoLogin: true
         |theme: "dark"
         |type: "user"
         |}""".stripMargin
    val HoconSuccess(actual) = HoconReader2.read[User[NotStored]](s, User.optional("root"))
    assert(User[NotStored](
      alias = "Alice",
      autologin = Some(EnabledEnum.enabled),
      theme = Some(Theme.dark),
      `type` = Some(UserType.user)
    ) === actual)
  }
}
