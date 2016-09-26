package com.github.zabbicook.operations

import com.github.zabbicook.entity.{EnabledEnum, User}
import com.github.zabbicook.operation.{UserConfig, UserOp}
import com.github.zabbicook.test.{TestUsers, UnitSpec}

class UserOpSpec extends UnitSpec with TestUsers {
  lazy val sut = new UserOp(cachedApi)

  "present and absent" should "create, delete and update user" in {
    val appended = UserConfig(User(alias = specName("userx"), autologin = Some(EnabledEnum.enabled), name = Some("Alice")),
      testUserGroups.take(2).map(_.userGroup.name).toSet, "password")

    def clean() = {
      cleanTestUsers()
      await(sut.absent(Seq(appended.user.alias)))
    }

    cleanRun(clean) {
      assert(Seq() === await(sut.findByAliases(testUsers.map(_.user.alias))))

      // creates
      {
        presentTestUsers()
        val founds = await(sut.findByAliases(testUsers.map(_.user.alias)))
        assert(founds.length === testUsers.length)
        testUsers.map { expected =>
          val actual = founds.find(_._1.alias == expected.user.alias).get
          assert(actual._1.shouldBeUpdated(expected.user) === false)
          assert(actual._2.map(_.name).toSet === expected.groupNames.toSet)
        }
      }

      // appends
      {
        val (_, report) = await(sut.present(testUsers :+ appended))
        assert(report.count === 1)
        assert(report.created.head.entityName === appended.user.entityName)
        val founds = await(sut.findByAliases((testUsers :+ appended).map(_.user.alias)))
        assert(founds.length === testUsers.length + 1)
        // represent does nothing
        val (_, report2) = await(sut.present(testUsers :+ appended))
        assert(report2.isEmpty)
      }

      // update
      {
        val modified = UserConfig(User(alias = specName("userx"), autologin = Some(EnabledEnum.disabled), name = Some("Bob")),
          Set(testUserGroups(0).userGroup.name), "password")

        val (_, report) = await(sut.present(testUsers :+ modified))
        assert(report.count === 1)
        assert(report.updated.head.entityName === modified.user.entityName)
        val Some(actual) = await(sut.findByAlias(modified.user.alias))
        assert(actual._1.alias === modified.user.alias)
        assert(actual._1.autologin.get === modified.user.autologin.get)
        assert(actual._1.name.get === modified.user.name.get)
        assert(actual._2.size === modified.groupNames.size)
        assert(actual._2.head.name === testUserGroups(0).userGroup.name)
      }

      // absent
      {
        val (_, report) = await(sut.absent((testUsers :+ appended).map(_.user.alias)))
        assert(report.count === testUsers.length + 1)
        report.deleted.take(report.count).foreach(e => assert(e.entityName === appended.user.entityName))
        assert(Seq() === await(sut.findByAliases(testUsers.map(_.user.alias))))
        // reabsent does nothing
        val (_, report2) = await(sut.absent((testUsers :+ appended).map(_.user.alias)))
        assert(report2.isEmpty())
      }
    }
  }

  "presentPassword" should "change passwords" in {
    def clean() = {
      cleanTestUsers()
    }

    cleanRun(clean) {
      presentTestUsers()
      val user = testUsers(0)
      val r = await(sut.present(user.copy(password = "NewPassword1234")))
      assert(r._2.count === 1)
      assert(r._2.updated.head.entityName === user.user.entityName)

      // now user can login with new password, so re-presentPassword does nothing
      val r2 = await(sut.present(user.copy(password = "NewPassword1234")))
      assert(r2._2.isEmpty())
    }
  }
}
