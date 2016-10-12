package com.github.zabbicook.entity.template

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.EntityState
import com.github.zabbicook.entity.host.HostGroup
import com.github.zabbicook.entity.item.Item
import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.util.TopologicalSortable

/**
  * Created by ryo_natori on 2016/10/12.
  */
case class TemplateSettings[TS <: EntityState, GS <: EntityState, LS <: EntityState](
  template: Template[TS],
  groups: Seq[HostGroup[GS]],
  linkedTemplates: Option[Seq[Template[LS]]]
) {
  def linkedTemplateHostNames: Option[Set[String]] = linkedTemplates.map(_.map(_.host).toSet)
  def groupsNames: Set[String] = groups.map(_.name).toSet
  def hostName = template.host
}
object TemplateSettings {
  type NotStoredAll = TemplateSettings[NotStored, NotStored, NotStored]

  implicit val topologicalSortable: TopologicalSortable[NotStoredAll] = TopologicalSortable[NotStoredAll] { (node, all) =>
    node.linkedTemplates match {
      case Some(links) =>
        links.map(linkHost => all.find(_.hostName == linkHost.host)).flatten
      case None => Seq()
    }
  }
}

case class TemplateSettingsConf(
  template: Template[NotStored],
  groupNames: Seq[String],
  linkedTemplateNames: Option[Seq[String]],
  items: Seq[Item[NotStored]]
) {
  def toTemplateSettings: TemplateSettings.NotStoredAll =
    TemplateSettings(
      template,
      groupNames.map(HostGroup.fromString),
      linkedTemplateNames.map(_.map(Template.fromString))
    )
}

object TemplateSettingsConf extends EntityCompanionMetaHelper {
  type NotStoredAll = TemplateSettings[NotStored, NotStored, NotStored]

  val meta = entity("Template settings and information that belongs to the template")(
    Template.required("template"),
    arrayOf("groupNames")(Template.required("groups")),
    array("groupNames")("groups")("(required) Names of host groups to add the template to."),
    array("linkedTemplateNames")("linkedTemplates")("Names of templates to be linked to the template."),
    arrayOf("items")(Item.required("items"))
  ) _
}
