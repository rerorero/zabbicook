package com.github.zabbicook.recipe

import com.github.zabbicook.entity.trigger.Severity
import com.github.zabbicook.entity.user.{MediaConfig, User, UserConfig}
import com.github.zabbicook.hocon.{HoconError, HoconReader, HoconSuccess}
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
          |    initialPassword: true
          |    media: [
          |       {
          |           enabled: true
          |           type: "script"
          |           period: "1-7,00:00-24:00"
          |           sendTo: "dest"
          |           severity: [
          |              information,
          |               warning
          |           ]
          |       }
          |    ]
          |  }
          |]
          |""".stripMargin
    val HoconSuccess(r) = HoconReader.read[Recipe](s, Recipe.optional("root"))
    assert(r.users === Some(Seq(UserConfig(
      user = User(alias = "Zabbicook-spec-Bob"),
      groupNames = Seq("zabbicook-spec usergroup2"),
      password = "pass5678",
      initialPassword = true,
      media = Some(Seq(
        MediaConfig(
          active = true,
          mediaType = "script",
          period = "1-7,00:00-24:00",
          sendto = "dest",
          severity = Seq(Severity.information, Severity.warning)
        )
      ))
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

  "Recipe" should "be failed in parsing if required params dont exist." in {
    val s =
      s"""
         |users: [
         |  {
         |    user {
         |      name: "name"
         |    }
         |    groups: ["zabbicook-spec usergroup2"]
         |    password: pass5678
         |  }
         |]
         |""".stripMargin
    val r = HoconReader.read[Recipe](s, Recipe.optional("root"))
    assert(r.asInstanceOf[HoconError.NotExist].meta.aliases.contains("alias"))
  }
}
