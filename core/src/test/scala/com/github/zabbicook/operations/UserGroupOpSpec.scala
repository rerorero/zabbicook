package com.github.zabbicook.operations

import com.github.zabbicook.entity.prop.{EnabledEnum, EnabledEnumZeroPositive}
import com.github.zabbicook.entity.user.{Permission, PermissionsOfHosts, UserGroup, UserGroupConfig}
import com.github.zabbicook.test.{TestConfig, TestUserGroups, UnitSpec}

class UserGroupOpSpec
  extends UnitSpec
  with TestConfig
  with TestUserGroups
{
  withTestOp { (ops, version) =>
    lazy val sut = ops.userGroup

    version + "present and absent" should "create and delete and update user groups" in {
      val appended = UserGroupConfig(UserGroup(name = specName("groupx")), Seq(PermissionsOfHosts(testHostGroups(0).name, Permission.readOnly)))

      def clean() = {
        cleanTestUserGroups(ops)
        await(sut.absent(Seq(appended.userGroup.name)))
      }

      cleanRun(clean) {
        assert(Seq() === await(sut.findByNames(testUserGroups.map(_.userGroup.name))))

        // creates
        {
          presentTestUserGroups(ops)
          val founds = await(sut.findByNames(testUserGroups.map(_.userGroup.name)))
          assert(founds.length === testUserGroups.length)
          testUserGroups.map { expected =>
            val actual = founds.find(_._1.name == expected.userGroup.name).get
            assert(actual._1.shouldBeUpdated(expected.userGroup) === false)
            assert(expected.permissions.size === actual._2.length)
          }
        }

        // appends
        {
          val report = await(sut.present(testUserGroups :+ appended))
          assert(report.count === 1)
          assert(report.created.head.entityName === appended.userGroup.entityName)
          val founds = await(sut.findByNames((testUserGroups :+ appended).map(_.userGroup.name)))
          assert(founds.length === testUserGroups.length + 1)
          // represent does nothing
          val report2 = await(sut.present(testUserGroups :+ appended))
          assert(report2.isEmpty)
        }

        // update
        {
          val modified = UserGroupConfig(
            UserGroup(name = specName("groupx"), debug_mode = Some(EnabledEnum.`true`), users_status = Some(EnabledEnumZeroPositive.disabled)),
            Seq(PermissionsOfHosts(testHostGroups(0).name, Permission.readWrite), PermissionsOfHosts(testHostGroups(1).name, Permission.readWrite))
          )
          val report = await(sut.present(testUserGroups :+ modified))
          assert(report.count === 1)
          assert(report.updated.head.entityName === modified.userGroup.entityName)
          val Some(actual) = await(sut.findByName(modified.userGroup.name))
          assert(actual._1.name === modified.userGroup.name)
          assert(actual._1.debug_mode.get === modified.userGroup.debug_mode.get)
          assert(actual._1.users_status.get === modified.userGroup.users_status.get)
          assert(actual._2.size === modified.permissions.size)
          assert(actual._2.forall(_.permission === Permission.readWrite))
        }

        // absent
        {
          val report = await(sut.absent((testUserGroups :+ appended).map(_.userGroup.name)))
          assert(report.count === testUserGroups.length + 1)
          report.deleted.take(report.count).foreach(e => assert(e.entityName === appended.userGroup.entityName))
          assert(Seq() === await(sut.findByNames(testUserGroups.map(_.userGroup.name))))
          // reabsent does nothing
          val report2 = await(sut.absent((testUserGroups :+ appended).map(_.userGroup.name)))
          assert(report2.isEmpty())
        }
      }
    }
  }
}
