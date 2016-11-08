package com.github.zabbicook.chef

import com.github.zabbicook.entity.template.TemplateSettingsConf
import com.github.zabbicook.operation.{Ops, Report}
import com.github.zabbicook.recipe.Recipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class Chef(api: Ops) {
  def present(recipe: Recipe): Future[Report] = {
    // TODO First, check the connectivity to zabbix api server via version api
    val templateSection = recipe.templates.getOrElse(Seq())
    Future {
      val reports = Seq(
        await(
          api.mediaType.present(recipe.mediaTypes.getOrElse(Seq())),
          api.hostGroup.present(recipe.hostGroups.getOrElse(Seq()))
        ),
        await(api.userGroup.present(recipe.userGroups.getOrElse(Seq()))),
        await(api.user.present(recipe.users.getOrElse(Seq()))),
        await(
          api.action.present(recipe.actions.getOrElse(Seq())),
          api.template.present(templateSection.map(_.toTemplateSettings))
        ),
        await(presentItems(templateSection)),
        await(
          presentGraphs(templateSection),
          presentTriggers(templateSection),
          api.host.present(recipe.hosts.getOrElse(Seq()))
        )
      )
      Report.flatten(reports)
    }
  }

  private[this] def await(futures: Future[Report]*): Report = {
    Await.result(Future.sequence(futures).map(Report.flatten), 60 seconds)
  }

  private[this] def presentItems(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => api.item.presentWithTemplate(s.template.host, s.items.getOrElse(Seq())))
      .map(Report.flatten)
  }

  private[this] def presentGraphs(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => api.graph.present(s.template.host, s.graphs.getOrElse(Seq())))
      .map(Report.flatten)
  }

  private[this] def presentTriggers(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => api.trigger.presentWithTemplate(s.template.host, s.triggers.getOrElse(Seq())))
      .map(Report.flatten)
  }
}
