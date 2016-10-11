package com.github.zabbicook.recipe

import com.github.zabbicook.entity.User
import com.github.zabbicook.hocon.{HoconError, HoconReader, HoconSuccess}
import com.github.zabbicook.operation.UserConfig
import com.github.zabbicook.test.UnitSpec

class RecipeSpec extends UnitSpec {
  import com.github.zabbicook.hocon.HoconReadsCompanion._
  import com.github.zabbicook.hocon.HoconReads.option

  "Recipe" should "parsed in Hocon format" in {
    val s =
      s"""
          |users: [
          |  {
          |    user {
          |      alias: "Zabbicook-spec-Bob"
          |    }
          |    groups: ["zabbicook-spec usergroup2"]
          |    password: pass5678
          |  }
          |]
          |""".stripMargin
    val HoconSuccess(r) = HoconReader.read[Recipe](s, Recipe.optional("root"))
    assert(r.users === Some(Seq(UserConfig(
      user = User(alias = "Zabbicook-spec-Bob"),
      groupNames = Seq("zabbicook-spec usergroup2"),
      password = "pass5678"
    ))))
  }

  "Recipe" should "be failed in parsing if having invalid schema" in {
    val s =
      s"""
         |users: [
         |  {
         |    unknown: "invalid"
         |    user {
         |      alias: "Zabbicook-spec-Bob"
         |    }
         |    groups: ["zabbicook-spec usergroup2"]
         |    password: pass5678
         |  }
         |]
         |""".stripMargin
    val r = HoconReader.read[Recipe](s, Recipe.optional("root"))
    assert(r.asInstanceOf[HoconError.UnrecognizedFields].invalids.toSet === Set("unknown"))
  }
}
