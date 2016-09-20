package com.github.zabbicook.api.user

import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.{EnabledEnum, EnabledEnumZeroPositive}
import com.github.zabbicook.test.{TestConfig, TestUserGroups, UnitSpec}
import com.github.zabbicook.user.{Permission, UserGroup, UserGroupOp}

class UserGroupOpSpec
  extends UnitSpec
  with TestConfig
  with TestUserGroups
{
  lazy val sut = new UserGroupOp(new ZabbixApi(apiConf))

  "present and absent" should "create and delete and update user groups" in {
    val appended = (UserGroup(name = specName("groupx")), Map(testHostGroups(0).name -> Permission.readOnly))

    def clean() = {
      cleanTestUserGroups()
      await(sut.absent(Seq(appended._1.name)))
    }

    cleanRun(clean) {
      assert(Seq() === await(sut.findByNames(testUserGroups.map(_._1.name))))

      // creates
      {
        presentTestUserGroups()
        val founds = await(sut.findByNames(testUserGroups.map(_._1.name)))
        assert(founds.length === testUserGroups.length)
        testUserGroups.map { expected =>
          val actual = founds.find(_._1.name == expected._1.name).get
          assert(actual._1.shouldBeUpdated(expected._1) === false)
          assert(expected._2.size === actual._2.length)
        }
      }

      // appends
      {
        val (_, report) = await(sut.present(testUserGroups :+ appended))
        assert(report.count === 1)
        assert(report.created.head.entityName === appended._1.entityName)
        val founds = await(sut.findByNames((testUserGroups :+ appended).map(_._1.name)))
        assert(founds.length === testUserGroups.length + 1)
        // represent does nothing
        val (_, report2) = await(sut.present(testUserGroups :+ appended))
        assert(report2.isEmpty)
      }

      // update
      {
        val modified = (UserGroup(name = specName("groupx"), debug_mode = Some(EnabledEnum.enabled), users_status = Some(EnabledEnumZeroPositive.disabled)),
          Map(testHostGroups(0).name -> Permission.readWrite, testHostGroups(1).name -> Permission.readWrite))
        val (_, report) = await(sut.present(testUserGroups :+ modified))
        assert(report.count === 1)
        assert(report.updated.head.entityName === modified._1.entityName)
        val Some(actual) = await(sut.findByName(modified._1.name))
        assert(actual._1.name === modified._1.name)
        assert(actual._1.debug_mode.get === modified._1.debug_mode.get)
        assert(actual._1.users_status.get === modified._1.users_status.get)
        assert(actual._2.size === modified._2.size)
        assert(actual._2.forall(_.permission === Permission.readWrite))
      }

      // absent
      {
        val (_, report) = await(sut.absent((testUserGroups :+ appended).map(_._1.name)))
        assert(report.count === testUserGroups.length + 1)
        report.deleted.take(report.count).foreach(e => assert(e.entityName === appended._1.entityName))
        assert(Seq() === await(sut.findByNames(testUserGroups.map(_._1.name))))
        // reabsent does nothing
        val (_, report2) = await(sut.absent((testUserGroups :+ appended).map(_._1.name)))
        assert(report2.isEmpty())
      }
    }
  }
}
