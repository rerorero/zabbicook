package com.github.zabbicook.chef

import com.github.zabbicook.hocon.TemplateSection
import com.github.zabbicook.operation.{OperationSet, Report}
import com.github.zabbicook.recipe.Recipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Chef(api: OperationSet) {
  def present(recipe: Recipe): Future[Report] = {
    // TODO First, check the connectivity to zabbix api server via version api
    for {
      rHostGroup <- api.hostGroup.present(recipe.hostGroups)
      // UserGroups require HostGroups
      rUserGroup <- api.userGroup.present(recipe.userGroupsAndPermissions.toSeq)
      // Users require UserGroups
      rUser <- api.user.present(recipe.users.toSeq)
      // Templates require HostGroups
      rTemplate <- api.template.present(recipe.templates.map(_.settings).toSeq)
      // items require Templates
      rItems <- presentItems(recipe.templates)
    } yield {
      rHostGroup + rUserGroup + rUser + rTemplate + rItems
    }
  }

  private[this] def presentItems(section: Seq[TemplateSection]): Future[Report] = {
    Future.traverse(section)(s => api.item.presentWithTemplate(s.settings.template.host, s.items))
      .map(Report.flatten)
  }
}
