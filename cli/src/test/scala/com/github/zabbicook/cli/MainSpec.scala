package com.github.zabbicook.cli

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity._
import com.github.zabbicook.operation.{OperationSet, TemplateSettings}
import com.github.zabbicook.test.{TestConfig, UnitSpec}

class MainSpec extends UnitSpec with TestConfig {

  def runMain(
    host: String,
    filePath: Option[String],
    debug: Boolean = false
  ): (Int, List[String]) = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[String]
    def mockedPrinter() = new Printer {
      override def print(msg: String): Unit = buf.append(msg)
    }

    val a = Seq("-a", host)
    val f = filePath.map(s => Seq("-f", s)).getOrElse(Seq())
    val d = if (debug) Seq("-d") else Seq()
    val printer = mockedPrinter()
    val code = await(new Main(printer).run(
      (a ++ f ++ d).toArray
    ))

    (code, buf.toList)
  }

  "CLI Main" should "parse and configure all from files" in {
    val op = new OperationSet(cachedApi)

    // expected
    val hostGroups = Seq(
      HostGroup(name = "zabbicook-spec hostgroup1"),
      HostGroup(name = "zabbicook-spec hostgroup2")
    )

    val userGroups: Seq[UserGroup[NotStored]] = Seq(
      UserGroup(name = "zabbicook-spec usergroup1", debug_mode = Some(false), users_status = Some(true)),
      UserGroup(name = "zabbicook-spec usergroup2")
    )

    val users: Seq[(User[NotStored], Seq[UserGroup[NotStored]])] = Seq(
      (User(alias = "Zabbicook-spec-Alice", autologin = Some(true), lang = Some("en"),
        theme = Some(Theme.dark), `type` = Some(UserType.superAdmin)), Seq(userGroups(0))),
      (User(alias = "Zabbicook-spec-Bob"), Seq(userGroups(1)))
    )

    val templates: Seq[TemplateSettings.NotStoredAll] = Seq(
      TemplateSettings(
        Template(host = "zabbicook-spec template 1", name = Some("template1"), description = Some("hello")),
        Seq(HostGroup(name = "zabbicook-spec hostgroup1")),
        Some(Seq(Template(host = "zabbicook-spec template2"), Template(host = "Template OS Linux")))
      ),
      TemplateSettings(
        Template(host = "zabbicook-spec template 2"),
        Seq(HostGroup(name = "zabbicook-spec hostgroup1"), HostGroup(name = "zabbicook-spec hostgroup2")),
        None
      )
    )

    def clean(): Unit = {
      await(op.template.absentTemplates(templates.map(_.template.host)))
      await(op.user.absent(users.map(_._1.alias)))
      await(op.userGroup.absent(userGroups.map(_.name)))
      await(op.hostGroup.absent(hostGroups.map(_.name)))
    }

    def check(): Unit = {
      // host groups
      val actualHostGroups = await(op.hostGroup.findByNames(hostGroups.map(_.name)))
      (actualHostGroups.sortBy((_.name)) zip hostGroups.sortBy((_.name))) foreach { case (actual, expected) =>
        assert(expected.name === actual.name)
      }
      // user groups
      val actualUserGroups = await(op.userGroup.findByNames(userGroups.map(_.name)))
      (actualUserGroups.sortBy((_._1.name)) zip userGroups.sortBy((_.name))) foreach { case (actual, expected) =>
        assert(false === actual._1.shouldBeUpdated(expected))
      }
      // users
      val actualUsers = await(op.user.findByAliases(users.map(_._1.alias)))
      (actualUsers.sortBy(_._1.alias) zip users.sortBy(_._1.alias)) foreach { case (actual, expected) =>
        assert(false === actual._1.shouldBeUpdated(expected._1))
        assert(expected._2.map(_.name).toSet === actual._2.map(_.name).toSet)
      }
      // templates
      val actualTemplates = await(op.template.findByHostnames(templates.map(_.template.host)))
      (actualTemplates.sortBy(_.template.host) zip templates.sortBy(_.template.host)) foreach { case (actual, expected) =>
        assert(false === actual.template.shouldBeUpdated(expected.template))
        assert(expected.groupsNames === actual.groupsNames)
        assert(expected.linkedTemplateHostNames === actual.linkedTemplateHostNames)
      }
    }

    cleanRun(clean) {
      val path = getClass.getResource("/mainspec/zabbicook.conf").getPath()
      val (code, _) = runMain("http://localhost:8080/", Some(path))
      assert(0 === code)
      check()

      // rerun does not affect
      val (code2, output2) = runMain("http://localhost:8080/", Some(path))
      assert(0 === code2)
      assert(1 === output2.length)
      check()
    }
  }
}
