package com.github.zabbicook.chef

import com.github.zabbicook.operation.{OperationSet, Report}
import com.github.zabbicook.recipe.Recipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Chef(api: OperationSet) {
  def present(recipe: Recipe): Future[Report] = {
    // TODO First, check the server connectivity with version api
    for {
      (hostGroupIds, rHostGroup) <- api.hostGroup.present(recipe.hostGroups)
      // UserGroup requires HostGroup
      (userGroupIds, rUserGroup) <- api.userGroup.present(recipe.userGroupsAndPermissions.toSeq)
      // UserOp requires UserGroup
      (userIds, rUser) <- api.user.present(recipe.users.toSeq)
    } yield {
      rHostGroup + rUserGroup + rUser
    }
  }
}
