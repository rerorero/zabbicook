package com.github.zabbicook.chef

import com.github.zabbicook.operation.TemplateSettings.TemplateSettingsConf
import com.github.zabbicook.operation.{OperationSet, Report}
import com.github.zabbicook.recipe.Recipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Chef(api: OperationSet) {
  def present(recipe: Recipe): Future[Report] = {
    // TODO First, check the connectivity to zabbix api server via version api
    println("natoring", recipe)
    for {
      rHostGroup <- api.hostGroup.present(recipe.hostGroups.getOrElse(Seq()))
      // UserGroups require HostGroups
      rUserGroup <- api.userGroup.present(recipe.userGroups.getOrElse(Seq()))
      // Users require UserGroups
      rUser <- api.user.present(recipe.users.getOrElse(Seq()))
      // Templates require HostGroups
      rTemplate <- api.template.present(recipe.templates.map(_.map(_.toTemplateSettings)).getOrElse(Seq()))
      // items require Templates
      rItems <- presentItems(recipe.templates.getOrElse(Seq()))
    } yield {
      rHostGroup + rUserGroup + rUser + rTemplate + rItems
    }
  }

  private[this] def presentItems(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => api.item.presentWithTemplate(s.template.host, s.items))
      .map(Report.flatten)
  }
}
