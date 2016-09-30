package com.github.zabbicook.test

import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.{Permission, UserGroup}
import com.github.zabbicook.operation.{UserGroupConfig, UserGroupOp}

trait TestUserGroups extends TestConfig with TestHostGroups { self: UnitSpec =>
  private[this] lazy val testUserGroupOp = new UserGroupOp(cachedApi)

  /**
    * you can override to customize generated users.
    */
  protected[this] val testUserGroups = Seq(
    UserGroupConfig(UserGroup(name = specName("group1")), Map(testHostGroups(0).name -> Permission.readOnly)),
    UserGroupConfig(UserGroup(name = specName("group2")), Map(testHostGroups(1).name -> Permission.readWrite))
  )

  def presentTestUserGroups(): (Seq[StoredId], Seq[StoredId])  = {
    val hostGroupIds = presentTestHostGroups()

    val (userGroupIds, _) = await(testUserGroupOp.present(testUserGroups))
    (userGroupIds, hostGroupIds)
  }

  def cleanTestUserGroups(): Unit = {
    await(testUserGroupOp.absent(testUserGroups.map(_.userGroup.name)))
    cleanTestHostGroups()
  }
}
