package com.github.zabbicook.chef

import com.github.zabbicook.entity.screen.ScreenSetting
import com.github.zabbicook.entity.template.TemplateSettingsConf
import com.github.zabbicook.operation.{Ops, Report}
import com.github.zabbicook.recipe.Recipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class Chef(api: Ops) {
  def present(recipe: Recipe): Future[Report] = {
    for {
      // First, check the connectivity to zabbix api server via version api
      version <- api.getVersion()
      _ <- Future.traverse(recipe.validatee)(_.validate(api, version))
      report <- presentAll(recipe)
    } yield report
  }

  private[this] def callOpt[T](opt: Option[T])(f: T => Future[Report]): Future[Report] = {
    opt.map(f).getOrElse(Future.successful(Report.empty()))
  }

  private[this] def presentAll(recipe: Recipe): Future[Report] = {
    val templateSection = recipe.templates
    Future {
      val reports = Seq(
        await(
          callOpt(recipe.mediaTypes)(api.mediaType.present),
          callOpt(recipe.hostGroups)(api.hostGroup.present)
        ),
        await(callOpt(recipe.userGroups)(api.userGroup.present)),
        await(callOpt(recipe.users)(api.user.present)),
        await(
          callOpt(recipe.actions)(api.action.present),
          callOpt(templateSection.map(_.map(_.toTemplateSettings)))(api.template.present)
        ),
        await(callOpt(templateSection)(presentItems)),
        await(
          callOpt(templateSection)(presentGraphs),
          callOpt(templateSection)(presentTriggers),
          callOpt(recipe.hosts)(api.host.present)
        ),
        await(callOpt(recipe.screens)(presentGlobalScreens))
      )
      Report.flatten(reports)
    }
  }

  private[this] def await(futures: Future[Report]*): Report = {
    Await.result(Future.sequence(futures).map(Report.flatten), 60 seconds)
  }

  private[this] def presentItems(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => callOpt(s.items)(api.item.presentWithTemplate(s.template.host, _)))
      .map(Report.flatten)
  }

  private[this] def presentGraphs(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => callOpt(s.graphs)(api.graph.present(s.template.host, _)))
      .map(Report.flatten)
  }

  private[this] def presentTriggers(section: Seq[TemplateSettingsConf]): Future[Report] = {
    Future.traverse(section)(s => callOpt(s.triggers)(api.trigger.presentWithTemplate(s.template.host, _)))
      .map(Report.flatten)
  }

  private[this] def presentGlobalScreens(section: Seq[ScreenSetting]): Future[Report] = {
    for {
      screenReport <- api.screen.present(section.map(_.screen))
      _ <- Future.traverse(section)(s => callOpt(s.items)(api.screenItem.present(s.screen.name, _, None)))
    } yield screenReport
  }
}
