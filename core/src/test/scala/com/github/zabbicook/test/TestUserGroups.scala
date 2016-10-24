package com.github.zabbicook.test

import com.github.zabbicook.entity.user.{Permission, PermissionsOfHosts, UserGroup, UserGroupConfig}
import com.github.zabbicook.operation.Ops

trait TestUserGroups extends TestConfig with TestHostGroups { self: UnitSpec =>
  /**
    * you can override to customize generated users.
    */
  protected[this] val testUserGroups = Seq(
    UserGroupConfig(UserGroup(name = specName("group1")), Seq(PermissionsOfHosts(testHostGroups(0).name, Permission.readOnly))),
    UserGroupConfig(UserGroup(name = specName("group2")), Seq(PermissionsOfHosts(testHostGroups(1).name, Permission.readWrite)))
  )

  def presentTestUserGroups(ops: Ops): Unit = {
    presentTestHostGroups(ops)
    await(ops.userGroup.present(testUserGroups))
  }

  def cleanTestUserGroups(ops: Ops): Unit = {
    await(ops.userGroup.absent(testUserGroups.map(_.userGroup.name)))
    cleanTestHostGroups(ops)
  }
}
