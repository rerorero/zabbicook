package com.github.zabbicook.test

import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.User
import com.github.zabbicook.operation.{UserConfig, UserOp}

trait TestUsers extends TestConfig with TestUserGroups{ self: UnitSpec =>
  private[this] lazy val testUserOp = new UserOp(cachedApi)

  /**
    * you can override to customize generated users.
    */
  protected[this] val testUsers: Seq[UserConfig] = Seq(
    UserConfig(User(alias = specName("user1")), Set(testUserGroups(0).userGroup.name), "password"),
    UserConfig(User(alias = specName("user2")), Set(testUserGroups(0).userGroup.name), "password"),
    UserConfig(User(alias = specName("user3")), Set(testUserGroups(1).userGroup.name), "password")
  )

  def presentTestUsers(): (Seq[StoredId], Seq[StoredId], Seq[StoredId]) = {
    val (userGroupIds, hostGroupIds) = presentTestUserGroups()

    val (userIds, _) = await(testUserOp.present(testUsers))
    (userIds, userGroupIds, hostGroupIds)
  }

  def cleanTestUsers(): Unit = {
    await(testUserOp.absent(testUsers.map(_.user.alias)))
    cleanTestUserGroups()
  }
}
