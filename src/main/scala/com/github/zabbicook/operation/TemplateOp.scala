package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.host.HostGroup
import com.github.zabbicook.entity.template.{Template, TemplateSettings}
import com.github.zabbicook.util.{Futures, TopologicalSort}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * template api
  *
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/template
  */
class TemplateOp(api: ZabbixApi, hostGroupOp: HostGroupOp) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  def findByHostname(hostname: String): Future[Option[TemplateSettings[Stored, Stored, Stored]]] = {
    val params = Json.obj()
      .filter("host" -> hostname)
      .outExtend()
      .prop("selectParentTemplates" -> "extend")
      .prop("selectGroups" -> "extend")
      .prop("selectHosts" -> "extend")
    api.requestSingleAs[JsObject]("template.get", params)
      .map(_.map(mapToTemplateSetting))
  }

  private[this] def mapToTemplateSetting(root: JsValue): TemplateSettings[Stored, Stored, Stored] = {
    val template = root.asOpt[Template[Stored]].getOrElse(sys.error(s"template.get returns unexpected formats.: ${Json.prettyPrint(root)}"))
    val parents = (root \ "parentTemplates").asOpt[Seq[Template[Stored]]].getOrElse(sys.error(s"template.get returns no parentTemplates"))
    val groups = (root \ "groups").asOpt[Seq[HostGroup[Stored]]].getOrElse(sys.error(s"template.get returns no hostGroups"))
    TemplateSettings(
      template = template,
      groups = groups,
      linkedTemplates = if (parents.isEmpty) None else Some(parents)
    )
  }

  def findByHostnames(hostnames: Seq[String]): Future[Seq[TemplateSettings[Stored, Stored, Stored]]] = {
    Future.traverse(hostnames)(findByHostname).map(_.flatten)
  }

  /**
    * If any one of templates does not exist, it fails.
    */
  def findByHostnamesAbsolutely(hostnames: Seq[String]): Future[Seq[TemplateSettings[Stored, Stored, Stored]]] = {
    findByHostnames(hostnames).map { results =>
      if (results.length < hostnames.length) {
        val notFounds = (hostnames.toSet -- results.map(_.hostName).toSet).mkString(",")
        throw NoSuchEntityException(s"No such templates: ${notFounds}")
      }
      results
    }
  }

  def findByHostnameAbsolutely(hostname: String): Future[TemplateSettings[Stored, Stored, Stored]] = {
    findByHostname(hostname).map {
      case Some(t) => t
      case None => throw NoSuchEntityException(s"No such templates: ${hostname}")
    }
  }

  /**
    * Creates a new template with the linked templates and host groups to which the template belongs
    */
  def create(template: TemplateSettings[NotStored, Stored, Stored]): Future[Report] = {
    val param = Json.toJson(template.template).as[JsObject]
      .prop("groups" -> template.groups.map(g => Json.obj("groupid" -> g.getStoredId)))
      .propIfDefined(template.linkedTemplates)("templates" -> _.map(t => Json.obj("templateid" -> t.getStoredId)))
    api.requestSingleId("template.create", param, "templateids")
      .map(id => Report.created(template.template.toStored(id)))
  }

  def delete(templates: Seq[Template[Stored]]): Future[Report] = {
    deleteEntities(api, templates, "template.delete", "templateids")
  }

  def update(
    current: TemplateSettings[Stored, Stored, Stored],
    update: TemplateSettings[NotStored, Stored, Stored]
  ): Future[Report] = {
    val param = update.template.toJsonForUpdate(current.template.getStoredId)
      .prop("groups" -> update.groups.map(g => Json.obj("groupid" -> g.getStoredId)))
      .propIfDefined(update.linkedTemplates)("templates" -> _.map(t => Json.obj("templateid" -> t.getStoredId.id)))
      .propIfDefined(current.linkedTemplates)("templates_clear" -> _.map(t => Json.obj("templateid" -> t.getStoredId.id)))
    api.requestSingleId("template.update", param, "templateids")
      .map(id => Report.updated(update.template.toStored(id)))
  }

  /**
    * Keep the status of the Teamplate to be constant.
    * If the template group specified hostname does not exist, create it.
    * If already exists, it fills the gap.
    */
  def present(template: TemplateSettings.NotStoredAll): Future[Report] = {
    for {
      presentGroups <- hostGroupOp.findByNamesAbsolutely(template.groupsNames.toSeq)
      presentLinkedTemplates <- template.linkedTemplates match {
        case Some(templates) =>
          findByHostnamesAbsolutely(templates.map(_.host)).map(_.map(_.template)).map { stored =>
            if (stored.isEmpty) None else Some(stored) // an empty value is treated as None
          }
        case None => Future(None)
      }
      storedTemplateSetting <- findByHostname(template.hostName)
      result <- storedTemplateSetting match {
        case Some(stored) =>
          val areGroupsSame = (stored.groupsNames == template.groupsNames)
          val areLinkedSame = (stored.linkedTemplateHostNames, template.linkedTemplateHostNames) match {
            case (Some(s), Some(t)) => s == t
            case _ => true
          }
          val areTemplateSame = !stored.template.shouldBeUpdated(template.template)
          if (areGroupsSame && areLinkedSame && areTemplateSame) {
            logger.debug("presentTemplate has nothing to update.")
            Future.successful(Report.empty())
          } else {
            val newTemplate = TemplateSettings[NotStored, Stored, Stored](
              template.template,
              presentGroups,
              presentLinkedTemplates
            )
            update(stored, newTemplate)
          }
        case None =>
          create(TemplateSettings(template.template, presentGroups, presentLinkedTemplates))
      }
    } yield {
      result
    }
  }

  def present(templates: Seq[TemplateSettings.NotStoredAll]): Future[Report] = {
    // sort templates by dependencies described in 'linkedTemplate' properties.
    TopologicalSort(templates) match {
      case Right(sorted) =>
        showStartInfo(logger, sorted.length, "templates").flatMap(_ =>
          Futures.sequential(sorted.reverse)(present).map(Report.flatten)
        )
      case Left(err) =>
        val entities = err.entities.map(_.hostName).mkString(",")
        Future.failed(BadReferenceException(s"Circular references in template settings.'linkedTemplates' of around $entities", err))
    }
  }

  def absent(hostnames: Seq[String]): Future[Report] = {
    findByHostnames(hostnames).flatMap(s => delete(s.map(_.template)))
  }
}
