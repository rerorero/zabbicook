package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Template.TemplateId
import com.github.zabbicook.entity.{HostGroup, Template}
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import com.github.zabbicook.util.{Futures, TopologicalSort, TopologicalSortable}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * template api
  *
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/template
  */
class TemplateOp(api: ZabbixApi) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  private[this] val hostGroupOp = new HostGroupOp(api)

  def findByHostname(hostname: String): Future[Option[TemplateSettings]] = {
    val params = Json.obj()
      .filter("host" -> hostname)
      .outExtend()
      .prop("selectParentTemplates" -> "extend")
      .prop("selectGroups" -> "extend")
      .prop("selectHosts" -> "extend")
    api.requestSingleAs[JsObject]("template.get", params).map(_.map(mapToTemplateSetting))
  }

  private[this] def mapToTemplateSetting(root: JsValue): TemplateSettings = {
    val template = root.asOpt[Template].getOrElse(sys.error(s"template.get returns unexpected formats.: ${Json.prettyPrint(root)}"))
    val parents = (root \ "parentTemplates").asOpt[Seq[Template]].getOrElse(sys.error(s"template.get returns no parentTemplates"))
    val groups = (root \ "groups").asOpt[Seq[HostGroup]].getOrElse(sys.error(s"template.get returns no hostGroups"))
    TemplateSettings(
      template = template,
      groups = groups,
      linkedTemplates = if (parents.isEmpty) None else Some(parents)
    )
  }

  def findByHostnames(hostnames: Seq[String]): Future[Seq[TemplateSettings]] = {
    Future.traverse(hostnames)(findByHostname).map(_.flatten)
  }

  /**
    * If any one of templates does not exist, it fails.
    */
  def findByHostnamesAbsolutely(hostnames: Seq[String]): Future[Seq[TemplateSettings]] = {
    findByHostnames(hostnames).map { results =>
      if (results.length < hostnames.length) {
        val notFounds = (hostnames.toSet -- results.map(_.template.host).toSet).mkString(",")
        throw NoSuchEntityException(s"No such templates: ${notFounds}")
      }
      results
    }
  }

  /**
    * Creates a new template with the linked templates and host groups to which the template belongs
    *
    * @return ids of created templates
    */
  def createTemplate(template: TemplateSettings): Future[(TemplateId, Report)] = {
    require(template.linkedTemplates.map(_.forall(_.templateid.isDefined)).getOrElse(true))
    require(template.groups.forall(_.groupid.isDefined))

    val param = Json.toJson(template.template.removeReadOnly).as[JsObject]
      .prop("groups" -> template.groups.map(g => Json.obj("groupid" -> g.groupid.get)))
      .propIfDefined(template.linkedTemplates)("templates" -> _.map(t => Json.obj("templateid" -> t.templateid.get)))
    api.requestSingleId[TemplateId]("template.create", param, "templateids")
      .map((_, Report.created(template.template)))
  }

  def deleteTemplates(templates: Seq[TemplateSettings]): Future[(Seq[TemplateId], Report)] = {
    if (templates.isEmpty) {
      Future.successful((Seq(), Report.empty()))
    } else {
      val ids = templates.map(t => t.template.templateid.getOrElse(sys.error(s"Template ${t.template.host} to be deleted has no id.")))
      val param = Json.toJson(ids)
      api.requestIds[TemplateId]("template.delete", param, "templateids")
        .map((_, Report.deleted(templates.map(_.template))))
    }
  }

  def updateTemplate(current: TemplateSettings, update: TemplateSettings): Future[(TemplateId, Report)] = {
    require(current.template.templateid.isDefined)
    require(current.linkedTemplates.map(_.forall(_.templateid.isDefined)).getOrElse(true))
    require(current.groups.forall(_.groupid.isDefined))
    require(update.template.templateid.isDefined)
    require(update.linkedTemplates.map(_.forall(_.templateid.isDefined)).getOrElse(true))
    require(update.groups.forall(_.groupid.isDefined))

    val param = Json.toJson(update.template).as[JsObject]
      .prop("groups" -> update.groups.map(g => Json.obj("groupid" -> g.groupid.get)))
      .propIfDefined(update.linkedTemplates)("templates" -> _.map(t => Json.obj("templateid" -> t.templateid.get)))
      .propIfDefined(current.linkedTemplates)("templates_clear" -> _.map(t => Json.obj("templateid" -> t.templateid.get)))
    api.requestSingleId[TemplateId]("template.update", param, "templateids")
      .map((_, Report.updated(update.template)))
  }

  /**
    * Keep the status of the Teamplate to be constant.
    * If the template group specified hostname does not exist, create it.
    * If already exists, it fills the gap.
    */
  def presentTemplate(template: TemplateSettings): Future[(TemplateId, Report)] = {
    for {
      presentGroups <- hostGroupOp.findByNamesAbsolutely(template.groups.map(_.name))
      presentLinkedTemplates <- template.linkedTemplates match {
        case Some(templates) =>
          findByHostnamesAbsolutely(templates.map(_.host)).map(_.map(_.template)).map { stored =>
            if (stored.isEmpty) None else Some(stored) // an empty value is treated as None
          }
        case None => Future(None)
      }
      storedTemplateSetting <- findByHostname(template.template.host)
      result <- storedTemplateSetting match {
        case Some(stored) =>
          val areGroupsSame = (stored.groups.map(_.name).toSet == template.groups.map(_.name).toSet)
          val areLinkedSame = compareOpt(stored.linkedTemplates, template.linkedTemplates) {
            _.map(_.host).toSet == _.map(_.host).toSet
          }
          val areTemplateSame = stored.template.shouldBeUpdated(template.template)
          val id = stored.template.templateid.getOrElse(sys.error(s"template.get returns no id."))
          if (areGroupsSame && areLinkedSame && areTemplateSame) {
            logger.debug("presentTemplate has nothing to update.")
            Future.successful((id, Report.empty()))
          } else {
            val newTemplate = TemplateSettings(
              template.template.copy(templateid = Some(id)),
              presentGroups,
              presentLinkedTemplates
            )
            updateTemplate(stored, newTemplate)
          }
        case None =>
          createTemplate(TemplateSettings(template.template, presentGroups, presentLinkedTemplates))
      }
    } yield {
      result
    }
  }

  def presentTemplates(templates: Seq[TemplateSettings]): Future[(Seq[TemplateId], Report)] = {
    // sort templates by dependencies described in 'linkedTemplate' properties.
    Future {
      val sortable = new TopologicalSortable[TemplateSettings] {
        override def dependencies(t: TemplateSettings): Iterable[TemplateSettings] = {
          t.linkedTemplates match {
            case Some(links) => links.map(linkHost => templates.find(_.template.host == linkHost)).flatten
            case None => Seq()
          }
        }
      }
      TopologicalSort(templates)(sortable)
    } flatMap {
      case Right(sorted) =>
        Futures.sequential(sorted)(presentTemplate).map(foldReports)
      case Left(err) =>
        val entities = err.entities.map(_.template.host).mkString(",")
        Future.failed(BadReferenceException(s"Cirucular references in template settings.'linkedTemplates' of around $entities", err))
    }
  }

  def absentTemplates(hostnames: Seq[String]): Future[(Seq[TemplateId], Report)] = {
    findByHostnames(hostnames).flatMap(deleteTemplates)
  }
}

case class TemplateSettings(
  template: Template,
  groups: Seq[HostGroup],
  linkedTemplates: Option[Seq[Template]]
)

object TemplateSettings {

  implicit val hoconReads: HoconReads[TemplateSettings] = {
    for {
      template <- of[Template]
      groups <- required[Seq[String]]("groups").map(_.map(HostGroup.fromString))
      linkedTemplates <- optional[Seq[String]]("linkedTemplates").map(_.map(_.map(Template.fromString)))
    } yield {
      TemplateSettings(template, groups, linkedTemplates)
    }
  }
}
