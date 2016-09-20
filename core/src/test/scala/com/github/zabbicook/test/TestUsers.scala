package com.github.zabbicook.test

import com.github.zabbicook.hostgroup.HostGroup.HostGroupId
import com.github.zabbicook.user.User.UserId
import com.github.zabbicook.user.UserGroup.UserGroupId
import com.github.zabbicook.user.{User, UserOp}

trait TestUsers extends TestConfig with TestUserGroups{ self: UnitSpec =>
  private[this] lazy val testUserOp = new UserOp(cachedApi)

  /**
    * you can override to customize generated users.
    */
  protected[this] val testUsers: Seq[(User, Seq[String], String)] = Seq(
    (User(alias = specName("user1")), Seq(testUserGroups(0)._1.name), "password"),
    (User(alias = specName("user2")), Seq(testUserGroups(0)._1.name), "password"),
    (User(alias = specName("user3")), Seq(testUserGroups(1)._1.name), "password")
  )

  def presentTestUsers(): (Seq[UserId], Seq[UserGroupId], Seq[HostGroupId]) = {
    val (userGroupIds, hostGroupIds) = presentTestUserGroups()

    val (userIds, _) = await(testUserOp.present(testUsers))
    (userIds, userGroupIds, hostGroupIds)
  }

  def cleanTestUsers(): Unit = {
    await(testUserOp.absent(testUsers.map(_._1.alias)))
    cleanTestUserGroups()
  }
}
