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
      // UserGroups requires HostGroups
      (userGroupIds, rUserGroup) <- api.userGroup.present(recipe.userGroupsAndPermissions.toSeq)
      // Users requires UserGroups
      (userIds, rUser) <- api.user.present(recipe.users.toSeq)
      // Template requires HostGroups
      (templateIds, rTemplate) <- api.template.presentTemplates(recipe.templates.toSeq)
    } yield {
      rHostGroup + rUserGroup + rUser + rTemplate
    }
  }
}
