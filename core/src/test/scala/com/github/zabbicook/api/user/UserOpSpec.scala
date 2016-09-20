package com.github.zabbicook.api.user

import com.github.zabbicook.entity.EnabledEnum
import com.github.zabbicook.test.{TestUsers, UnitSpec}
import com.github.zabbicook.user.{User, UserOp}

class UserOpSpec extends UnitSpec with TestUsers {
  lazy val sut = new UserOp(cachedApi)

  "present and absent" should "create, delete and update user" in {
    val appended = (User(alias = specName("userx"), autologin = Some(EnabledEnum.enabled), name = Some("Alice")),
      testUserGroups.take(2).map(_._1.name), "password")

    def clean() = {
      cleanTestUsers()
      await(sut.absent(Seq(appended._1.alias)))
    }

    cleanRun(clean) {
      assert(Seq() === await(sut.findByAliases(testUsers.map(_._1.alias))))

      // creates
      {
        presentTestUsers()
        val founds = await(sut.findByAliases(testUsers.map(_._1.alias)))
        assert(founds.length === testUsers.length)
        testUsers.map { expected =>
          val actual = founds.find(_._1.alias == expected._1.alias).get
          assert(actual._1.shouldBeUpdated(expected._1) === false)
          assert(actual._2.map(_.name).toSet === expected._2.toSet)
        }
      }

      // appends
      {
        val (_, report) = await(sut.present(testUsers :+ appended))
        assert(report.count === 1)
        assert(report.created.head.entityName === appended._1.entityName)
        val founds = await(sut.findByAliases((testUsers :+ appended).map(_._1.alias)))
        assert(founds.length === testUsers.length + 1)
        // represent does nothing
        val (_, report2) = await(sut.present(testUsers :+ appended))
        assert(report2.isEmpty)
      }

      // update
      {
        val modified = (User(alias = specName("userx"), autologin = Some(EnabledEnum.disabled), name = Some("Bob")),
          Seq(testUserGroups(0)._1.name), "password")

        val (_, report) = await(sut.present(testUsers :+ modified))
        assert(report.count === 1)
        assert(report.updated.head.entityName === modified._1.entityName)
        val Some(actual) = await(sut.findByAlias(modified._1.alias))
        assert(actual._1.alias === modified._1.alias)
        assert(actual._1.autologin.get === modified._1.autologin.get)
        assert(actual._1.name.get === modified._1.name.get)
        assert(actual._2.size === modified._2.size)
        assert(actual._2.head.name === testUserGroups(0)._1.name)
      }

      // absent
      {
        val (_, report) = await(sut.absent((testUsers :+ appended).map(_._1.alias)))
        assert(report.count === testUsers.length + 1)
        report.deleted.take(report.count).foreach(e => assert(e.entityName === appended._1.entityName))
        assert(Seq() === await(sut.findByAliases(testUsers.map(_._1.alias))))
        // reabsent does nothing
        val (_, report2) = await(sut.absent((testUsers :+ appended).map(_._1.alias)))
        assert(report2.isEmpty())
      }
    }
  }
}
