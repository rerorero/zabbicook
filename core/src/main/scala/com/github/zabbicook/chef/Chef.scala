package com.github.zabbicook.chef

import com.github.zabbicook.entity.template.TemplateSettingsConf
import com.github.zabbicook.operation.{Ops, Report}
import com.github.zabbicook.recipe.Recipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Chef(api: Ops) {
  def present(recipe: Recipe): Future[Report] = {
    // TODO First, check the connectivity to zabbix api server via version api
    for {
      rHostGroup <- api.hostGroup.present(recipe.hostGroups.getOrElse(Seq()))
      // UserGroups require HostGroups
      rUserGroup <- api.userGroup.present(recipe.userGroups.getOrElse(Seq()))
      // Users require UserGroups
      rUser <- api.user.present(recipe.users.getOrElse(Seq()))
      // Templates require HostGroups
      templateSection = recipe.templates.getOrElse(Seq())
      rTemplate <- api.template.present(templateSection.map(_.toTemplateSettings))
      // items require Templates
      rItems <- presentItems(templateSection)
      // graphs require Template
      rGraphs <- presentGraphs(templateSection)
    } yield {
      rHostGroup + rUserGroup + rUser + rTemplate + rItems + rGraphs
    }
  }

  private[this] def presentItems(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => api.item.presentWithTemplate(s.template.host, s.items.getOrElse(Seq())))
      .map(Report.flatten)
  }

  private[this] def presentGraphs(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => api.graph.present(s.template.host, s.graphs.getOrElse(Seq())))
      .map(Report.flatten)
  }
}
