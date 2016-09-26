package com.github.zabbicook.cli

import com.github.zabbicook.entity._
import com.github.zabbicook.operation.OperationSet
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

    val userGroups = Seq(
      UserGroup(name = "zabbicook-spec usergroup1", debug_mode = Some(false), users_status = Some(true)),
      UserGroup(name = "zabbicook-spec usergroup2")
    )
    val users: Seq[(User, Seq[UserGroup])] = Seq(
      (User(alias = "Zabbicook-spec-Alice", autologin = Some(true), lang = Some("en"),
        theme = Some(Theme.dark), `type` = Some(UserType.superAdmin)), Seq(userGroups(0))),
      (User(alias = "Zabbicook-spec-Bob"), Seq(userGroups(1)))
    )

    def clean(): Unit = {
      await(op.user.absent(users.map(_._1.alias)))
      await(op.userGroup.absent(userGroups.map(_.name)))
      await(op.hostGroup.absent(hostGroups.map(_.name)))
    }

    def check(): Unit = {
      // host groups
      val actualHostGroups = await(op.hostGroup.findByNames(hostGroups.map(_.name)))
      (actualHostGroups.sortBy((_.name)) zip hostGroups.sortBy((_.name))) foreach { case (actual, expected) =>
        assert(actual.name === expected.name)
      }
      // user groups
      val actualUserGroups = await(op.userGroup.findByNames(userGroups.map(_.name)))
      (actualUserGroups.sortBy((_._1.name)) zip userGroups.sortBy((_.name))) foreach { case (actual, expected) =>
        assert(actual._1.shouldBeUpdated(expected) === false)
      }
      // users
      val actualUsers = await(op.user.findByAliases(users.map(_._1.alias)))
      (actualUsers.sortBy(_._1.alias) zip users.sortBy(_._1.alias)) foreach { case (actual, expected) =>
        assert(actual._1.shouldBeUpdated(expected._1) === false)
        assert(actual._2.map(_.name).toSet === expected._2.map(_.name).toSet)
      }
    }

    cleanRun(clean) {
      val path = getClass.getResource("/mainspec/zabbicook.conf").getPath()
      val (code, _) = runMain("http://localhost:8080/", Some(path))
      assert(code === 0)
      check()

      // rerun does not affect
      val (code2, output2) = runMain("http://localhost:8080/", Some(path))
      assert(code2 === 0)
      assert(output2.length === 1)
      check()
    }
  }
}
