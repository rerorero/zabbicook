package com.github.zabbicook.test

import com.github.zabbicook.entity.trigger.Severity
import com.github.zabbicook.entity.user.{MediaConfig, User, UserConfig}
import com.github.zabbicook.operation.UserOp

trait TestUsers extends TestConfig with TestUserGroups with TestMedia { self: UnitSpec =>
  protected[this] lazy val testUserOp = new UserOp(cachedApi, testUserGroupOp, testMediaTypeOp)

  protected[this] val userMedias1 = Seq(
    MediaConfig(active=true, testMediaTypes(0).description, period = "1-7,00:00-24:00", sendto = "dest", severity = Seq(Severity.information, Severity.warning))
  )

  /**
    * you can override to customize generated users.
    */
  protected[this] val testUsers: Seq[UserConfig] = Seq(
    UserConfig(User(alias = specName("user1")), Seq(testUserGroups(0).userGroup.name), "password", initialPassword = false, None),
    UserConfig(User(alias = specName("user2")), Seq(testUserGroups(0).userGroup.name), "password", initialPassword = true, Some(userMedias1)),
    UserConfig(User(alias = specName("user3")), Seq(testUserGroups(1).userGroup.name), "password", initialPassword = false, None)
  )

  def presentTestUsers(): Unit = {
    presentTestMediaTypes()
    presentTestUserGroups()
    await(testUserOp.present(testUsers))
  }

  def cleanTestUsers(): Unit = {
    await(testUserOp.absent(testUsers.map(_.user.alias)))
    cleanTestUserGroups()
    cleanTestMediaTypes()
  }
}
