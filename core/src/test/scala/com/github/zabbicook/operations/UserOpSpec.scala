package com.github.zabbicook.operations

import com.github.zabbicook.entity.prop.EnabledEnum
import com.github.zabbicook.entity.trigger.Severity
import com.github.zabbicook.entity.user.{MediaConfig, User, UserConfig}
import com.github.zabbicook.hocon.{HoconReader, HoconSuccess}
import com.github.zabbicook.test.{TestUsers, UnitSpec}
import com.typesafe.config.ConfigFactory
import com.github.zabbicook.hocon.HoconReadsCompanion._
import com.github.zabbicook.hocon.HoconReads.option

class UserOpSpec extends UnitSpec with TestUsers {
  lazy val sut = testUserOp

  "present and absent" should "create, delete and update user" in {
    val appended = UserConfig(
      User(alias = specName("userx"), autologin = Some(EnabledEnum.enabled), name = Some("Alice")),
      testUserGroups.take(2).map(_.userGroup.name),
      password = "password",
      initialPassword = false,
      media = Some(Seq(
        MediaConfig(
          active = true, testMediaTypes(1).description, period="1-7,00:00-24:00", sendto="test", severity=Seq(Severity.average)
        )
      ))
    )

    def clean() = {
      await(sut.absent(Seq(appended.user.alias)))
      cleanTestUsers()
    }

    def check(conf: Seq[UserConfig]): Unit = {
      val founds = await(sut.findByAliases(conf.map(_.user.alias)))
      assert(founds.length === conf.length)
      conf.map { expected =>
        val actual = founds.find(_._1.alias == expected.user.alias).get
        assert(actual._1.shouldBeUpdated(expected.user) === false)
        assert(actual._2.map(_.name).toSet === expected.groupNames.toSet)
        val mediaActuals = await(sut.findUserMedias(actual._1.getStoredId))
        val expectedMedias = await(sut.mediaConfigToNotStoredMedia(expected.media.getOrElse(Seq()),actual._1.alias))
        assert(expectedMedias.length === mediaActuals.length)
        expectedMedias.forall(e => mediaActuals.exists(_.isSame(e)))
      }
    }

    cleanRun(clean) {
      assert(Seq() === await(sut.findByAliases(testUsers.map(_.user.alias))))

      // creates
      {
        presentTestUsers()
        check(testUsers)
      }

      // appends
      {
        val report = await(sut.present(testUsers :+ appended))
        assert(report.count === 2) // user + mediatypes
        assert(report.created.exists(_.entityName == appended.user.entityName))
        check(testUsers :+ appended)
        // represent does nothing
        val report2 = await(sut.present(testUsers :+ appended))
        assert(report2.isEmpty)
      }

      // update
      {
        val modified = UserConfig(
          User(alias = specName("userx"), autologin = Some(EnabledEnum.disabled), name = Some("Bob")),
          Seq(testUserGroups(0).userGroup.name),
          password = "password",
          initialPassword = false,
          media = Some(Seq(
            MediaConfig(
              active = true, testMediaTypes(1).description, period="1-7,00:00-24:00", sendto="test", severity=Seq(Severity.information)
            )
          ))
        )

        val report = await(sut.present(testUsers :+ modified))
        assert(report.count > 1)
        assert(report.updated.exists(_.entityName == modified.user.entityName))
        check(testUsers :+ modified)
      }

      // absent
      {
        val report = await(sut.absent((testUsers :+ appended).map(_.user.alias)))
        assert(report.count === testUsers.length + 1)
        report.deleted.take(report.count).foreach(e => assert(e.entityName === appended.user.entityName))
        assert(Seq() === await(sut.findByAliases(testUsers.map(_.user.alias))))
        // reabsent does nothing
        val report2 = await(sut.absent((testUsers :+ appended).map(_.user.alias)))
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
      assert(r.count === 1)
      assert(r.updated.head.entityName === user.user.entityName)

      // now user can login with new password, so re-presentPassword does nothing
      val r2 = await(sut.present(user.copy(password = "NewPassword1234")))
      assert(r2.isEmpty())

      // If 'initialPassword' is true, the password cannot be changed.
      val r3 = await(sut.present(user.copy(password = "NewNewPassword", initialPassword = true)))
      assert(r3.count === 0)
    }
  }

  "UserConfig" should "parsed from Hocon" in {
    val s = s"""{
                |  user {
                |    alias: "Alice"
                |    lang: "en"
                |  }
                |  groups: ["g1", "g2"]
                |  password: "pass"
                |  initialPassword: false
                |}""".stripMargin
    val actual = HoconReader.read[UserConfig](ConfigFactory.parseString(s), UserConfig.optional("root"))
    assert(actual === HoconSuccess(UserConfig(
      user = User(alias = "Alice", lang = Some("en")),
      groupNames = Seq("g1", "g2"),
      password = "pass",
      initialPassword = false,
      media = None
    )))
  }
}
