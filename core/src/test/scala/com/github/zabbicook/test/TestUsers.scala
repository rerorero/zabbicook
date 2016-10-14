package com.github.zabbicook.test

import com.github.zabbicook.entity.user.{User, UserConfig}
import com.github.zabbicook.operation.{HostGroupOp, UserGroupOp, UserOp}

trait TestUsers extends TestConfig with TestUserGroups{ self: UnitSpec =>
  private[this] lazy val testUserOp = new UserOp(cachedApi, new UserGroupOp(cachedApi, new HostGroupOp(cachedApi)))

  /**
    * you can override to customize generated users.
    */
  protected[this] val testUsers: Seq[UserConfig] = Seq(
    UserConfig(User(alias = specName("user1")), Seq(testUserGroups(0).userGroup.name), "password"),
    UserConfig(User(alias = specName("user2")), Seq(testUserGroups(0).userGroup.name), "password"),
    UserConfig(User(alias = specName("user3")), Seq(testUserGroups(1).userGroup.name), "password")
  )

  def presentTestUsers(): Unit = {
    presentTestUserGroups()
    await(testUserOp.present(testUsers))
  }

  def cleanTestUsers(): Unit = {
    await(testUserOp.absent(testUsers.map(_.user.alias)))
    cleanTestUserGroups()
  }
}
