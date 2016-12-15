package com.github.zabbicook.entity.screen

import com.github.zabbicook.api.Version
import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop.{EnabledEnum, EntityCompanionMetaHelper, IntProp}
import com.github.zabbicook.entity.{EntityException, Validate}
import com.github.zabbicook.operation.Ops

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class ScreenItemResource(
  template: Option[String] = None,
  name: String
)

object ScreenItemResource extends EntityCompanionMetaHelper {
  import ScreenResourceType._
  override val meta = entity(
    s"""Resource name of the object displayed on the screen item.
       |Depending on the type of a screen item, this property can reference different objects.
       |Required for data overview, graph, map, plain text, screen, simple graph, host issues, host group issues and trigger overview screen items.
       |Unused by local and server time clocks, history of actions, history of events, hosts info, status of Zabbix, system status and URL screen items."""
  )(
    value("name")("name")("(required) Resource name (such as item and graph)."),
    value("template")("template")(
      s"""The name of the template to which the resource belongs.
         |Required for the following resource types:
         |$graph, $simpleGraph, $plainText
       """.stripMargin)
  ) _
}

case class ScreenItemSetting(
  resourcetype: ScreenResourceType,
  application: Option[String] = None,
  colspan: Option[IntProp] = None,
  dynamic: Option[EnabledEnum] = None,
  elements: Option[IntProp] = None,
  halign: Option[HAlign] = None,
  height: Option[IntProp] = None,
  max_columns: Option[IntProp] = None,
  resource: Option[ScreenItemResource] = None,
  rowspan: Option[IntProp] = None,
  sort_triggers: Option[SortTrigger] = None,
  style: Option[ScreenItemStyle] = None,
  url: Option[String] = None,
  valign: Option[VAlign] = None,
  width: Option[IntProp] = None,
  x: Option[IntProp] = None,
  y: Option[IntProp] = None
){
  def toScreenItem(screenId: StoredId, resourceId: Option[StoredId]): ScreenItem[NotStored] = {
    ScreenItem(
      screenitemid = NotStoredId,
      resourcetype = resourcetype,
      screenid = screenId,
      application = application,
      colspan = colspan,
      dynamic = dynamic,
      elements = elements,
      halign = halign,
      height = height,
      max_columns = max_columns,
      //resourceid	string	ID of the object displayed on the screen item. Depending on the type of a screen item, the resourceid property can reference different objects.
      // Required for data overview, graph, map, plain text, screen, simple graph and trigger overview screen items. Unused by local and server time clocks, history of actions, history of events, hosts info, status of Zabbix, system status and URL screen items.
      resourceid = resourceId,
      rowspan = rowspan,
      sort_triggers = sort_triggers,
      style = style,
      url = url,
      valign = valign,
      width = width,
      x = x,
      y = y
    )
  }

  def validate(templateName: Option[String]): Try[Unit] = {
    (ScreenResourceType.requiredResourceId(resourcetype), resource, templateName) match {
      case (true, None,                                 _) =>
        Failure(EntityException(s"'resource' field is required for screen 'items' entity.(with x = ${x.getOrElse("undefined")}, y = ${y.getOrElse("undefined")})"))
      case (true, Some(ScreenItemResource(None, name)), None) =>
        Failure(EntityException(s"'template' filed is required for 'resource'(name=$name). (in screen item with x = ${x.getOrElse("undefined")}, y = ${y.getOrElse("undefined")})"))
      case (true, Some(ScreenItemResource(None, _)),    Some(_)) =>
        Success(())
      case (true, Some(ScreenItemResource(Some(_), _)), _) =>
        Success(())
      case (false, Some(_), _) =>
        Failure(EntityException(s"'resource' fields are not required for 'screen' entity.(with x = ${x.getOrElse("undefined")}, y = ${y.getOrElse("undefined")})"))
      case (false, None,    _) =>
        Success(())
    }
  }

  def key: ScreenItem.Key = (x.getOrElse(0), y.getOrElse(0))
}

object ScreenItemSetting extends EntityCompanionMetaHelper{
  override val meta = entity("The screen item object defines an element displayed on a screen.")(
    ScreenResourceType.meta("resourcetype")("resourceType", "type"),
    value("application")("application")(
      s"""Application or part of application name by which data in screen item can be filtered.
          |Applies to resource types: '${ScreenResourceType.dataOverview.toString}' and '${ScreenResourceType.triggersOverview.toString}'""".stripMargin),
    value("colspan")("columnSpan","column")("Number of columns the screen item will span across. Default: 1."),
    EnabledEnum.metaWithDesc("dynamic")("dynamic")(
      """Whether the screen item is dynamic.
        |Default: false (not dynamic)""".stripMargin),
    value("elements")("showLines")("Number of lines to display on the screen item. Default: 25."),
    HAlign.meta("halign")("horizontalAlign","halign"),
    value("height")("height")("Height of the screen item in pixels. Default: 200."),
    value("max_columns")("maxColumns")("Specifies the maximum amount of columns a graph prototype or simple graph prototype screen element can have. Default: 3."),
    ScreenItemResource.optional("resource"),
    value("rowspan")("rowSpan", "row")("Number or rows the screen item will span across. Default: 1."),
    SortTrigger.meta("sort_triggers")("sortTrigger", "sort"),
    ScreenItemStyle.meta("style")("style", "hostsLocation", "timeType", "showsAsHtml"),
    value("url")("url", "URL")(s"URL of the webpage to be displayed in the screen item. Used by ${ScreenResourceType.URL} screen items."),
    VAlign.meta("valign")("verticalAlign", "valign"),
    value("width")("width")("Width of the screen item in pixels. Default: 320."),
    value("x")("x","xCoordinates")("X-coordinates of the screen item on the screen, from left to right. Default: 0."),
    value("y")("y","yCoordinates")("Y-coordinates of the screen item on the screen, from top to bottom. Default: 0.")
  ) _
}

case class ScreenSetting(
  screen: Screen[NotStored],
  items: Option[Seq[ScreenItemSetting]]
) extends Validate {
  // TODO: check the duplicates of (column, row) pair
  override def validate(op: Ops, version: Version): Future[Unit] = {
    Future {
      items.getOrElse(Seq()).map(_.validate(None).get) // unsafe.
    }
  }
}

object ScreenSetting extends EntityCompanionMetaHelper {
  override val meta = entity("Screen setting.")(
    Screen.required("screen"),
    arrayOf("items")(ScreenItemSetting.optional("items"))
  ) _
}
