package com.github.zabbicook.test

import com.github.zabbicook.hostgroup.HostGroup.HostGroupId
import com.github.zabbicook.user.UserGroup.UserGroupId
import com.github.zabbicook.user.{Permission, UserGroup, UserGroupOp}

trait TestUserGroups extends TestConfig with TestHostGroups { self: UnitSpec =>
  private[this] lazy val testUserGroupOp = new UserGroupOp(cachedApi)

  /**
    * you can override to customize generated users.
    */
  protected[this] val testUserGroups = Seq(
    (UserGroup(name = specName("group1")), Map(testHostGroups(0).name -> Permission.readOnly)),
    (UserGroup(name = specName("group2")), Map(testHostGroups(1).name -> Permission.readWrite))
  )

  def presentTestUserGroups(): (Seq[UserGroupId], Seq[HostGroupId])  = {
    val hostGroupIds = presentTestHostGroups()

    val (userGroupIds, _) = await(testUserGroupOp.present(testUserGroups))
    (userGroupIds, hostGroupIds)
  }

  def cleanTestUserGroups(): Unit = {
    await(testUserGroupOp.absent(testUserGroups.map(_._1.name)))
    cleanTestHostGroups()
  }
}
