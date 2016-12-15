package com.github.zabbicook.entity.screen

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop._
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json._

sealed abstract class ScreenResourceType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]
object ScreenResourceType extends IntEnumPropCompanion[ScreenResourceType] {
  override val values: Seq[ScreenResourceType] = Seq(
   graph,
   simpleGraph,
//   map,
   plainText,
   hostsInfo,
   triggersInfo,
   statusOfZabbix,
   clock,
   screen,
   triggersOverview,
   dataOverview,
   URL,
   historyOfActions,
   historyOfEvents,
   hostGroupIssues,
   systemStatus,
   hostIssues,
//   simpleGraphPrototype,
//   graphPrototype,
   unknown
  )

  // Required for data overview, graph, map, plain text, screen, simple graph and trigger overview screen items.
  // Unused by local and server time clocks, history of actions, history of events, hosts info, status of Zabbix, system status and URL screen items.
  private[this] val typesWhichRequireResourceId: Seq[ScreenResourceType] = Seq(
    graph,
    simpleGraph,
//    map,
    plainText,
    //hostsInfo,
    triggersInfo,
    //statusOfZabbix,
    //clock,
    screen,
    triggersOverview,
    dataOverview,
    //URL,
    //historyOfActions,
    //historyOfEvents,
    hostGroupIssues,
    systemStatus,
    hostIssues
//    simpleGraphPrototype,
//    graphPrototype
    //unknown
  )

  def requiredResourceId(typ: ScreenResourceType): Boolean = typesWhichRequireResourceId.contains(typ)

  override val description: String = "(required) Type of screen item."

  case object graph extends ScreenResourceType(0, "graph")
  case object simpleGraph extends ScreenResourceType(1, "graph")
//  case object map extends ScreenResourceType(2, "map")  TODO: support maps
  case object plainText extends ScreenResourceType(3, "plain text")
  case object hostsInfo extends ScreenResourceType(4, "hosts info")
  case object triggersInfo extends ScreenResourceType(5, "triggers info")
  case object statusOfZabbix extends ScreenResourceType(6, "status of Zabbix")
  case object clock extends ScreenResourceType(7, "clock")
  case object screen extends ScreenResourceType(8, "screen")
  case object triggersOverview extends ScreenResourceType(9, "triggers overview")
  case object dataOverview extends ScreenResourceType(10, "data overview")
  case object URL extends ScreenResourceType(11, "URL")
  case object historyOfActions extends ScreenResourceType(12, "history of actions")
  case object historyOfEvents extends ScreenResourceType(13, "history of events")
  case object hostGroupIssues extends ScreenResourceType(14, "latest host group issues")
  case object systemStatus extends ScreenResourceType(15, "system status")
  case object hostIssues extends ScreenResourceType(16, "latest host issues")
//  case object simpleGraphPrototype extends ScreenResourceType(19, "simple graph prototype") TODO: support item prototypes
//  case object graphPrototype extends ScreenResourceType(20, "graph prototype") TODO: support graph prototypes
  case object unknown extends ScreenResourceType(-1, "unknown")
}

sealed abstract class HAlign(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]
object HAlign extends IntEnumPropCompanion[HAlign] {
  override val values: Seq[HAlign] = Seq(center,left,right,unknown)
  override val description: String = "Specifies how the screen item must be aligned horizontally in the cell."
  case object center extends HAlign(0, "(default) center")
  case object left extends HAlign(1, "left")
  case object right extends HAlign(2, "right")
  case object unknown extends HAlign(-1, "unknown")
}

sealed abstract class VAlign(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]
object VAlign extends IntEnumPropCompanion[VAlign] {
  override val values: Seq[VAlign] = Seq(middle,top,bottom,unknown)
  override val description: String = "Specifies how the screen item must be aligned vertically in the cell."
  case object middle extends VAlign(0, "(default) middle")
  case object top extends VAlign(1, "top")
  case object bottom extends VAlign(2, "bottom")
  case object unknown extends VAlign(-1, "unknown")
}

sealed abstract class SortTrigger(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]
object SortTrigger extends IntEnumPropCompanion[SortTrigger] {
  import ScreenResourceType._
  override val values: Seq[SortTrigger] = Seq(
   timeAsc,
   timeDesc,
   typeAsc,
   typeDesc,
   statusAsc,
   statusDesc,
   retriesLeftAsc,
   retriesLeftDesc,
   recipientAsc,
   recipientDesc,
   lastChange,
   severity,
   host,
   unknown
  )
  override val description: String =
    s"""Order in which actions or triggers must be sorted.
       |Applies to resource types: '${historyOfActions}', '${hostIssues}', '${hostGroupIssues}'""".stripMargin

  case object timeAsc extends SortTrigger(3, s"time, ascending: for '${historyOfActions}'")
  case object timeDesc extends SortTrigger(4, s"time, descending: for '${historyOfActions}'")
  case object typeAsc extends SortTrigger(5, s"type, ascending: for '${historyOfActions}'")
  case object typeDesc extends SortTrigger(6, s"type, descending: for '${historyOfActions}'")
  case object statusAsc extends SortTrigger(7, s"status, ascending: for '${historyOfActions}'")
  case object statusDesc extends SortTrigger(8, s"status, descending: for '${historyOfActions}'")
  case object retriesLeftAsc extends SortTrigger(9, s"retries left, ascending: for '${historyOfActions}'")
  case object retriesLeftDesc extends SortTrigger(10, s"retries left, descending: for '${historyOfActions}'")
  case object recipientAsc extends SortTrigger(11, s"recipient, ascending: for '${historyOfActions}'")
  case object recipientDesc extends SortTrigger(12, s"recipient, descending: for '${historyOfActions}'")

  case object lastChange extends SortTrigger(0, s"(default) last change, descending: for '${hostIssues}', '${hostGroupIssues}'")
  case object severity extends SortTrigger(1, s"severity, descending: for '${hostIssues}', '${hostGroupIssues}'")
  case object host extends SortTrigger(2, s"host, ascending: for '${hostIssues}', '${hostGroupIssues}'")
  case object unknown extends SortTrigger(-1, "unknown")
}

sealed abstract class ScreenItemStyle(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]
object ScreenItemStyle extends IntEnumPropCompanion[ScreenItemStyle] {
  import ScreenResourceType._
  override val values: Seq[ScreenItemStyle] = Seq(style1,style2,style3,unknown)
  override val description: String =
    s"""
      |Screen item display option.
      |
      |Possible values for '${dataOverview}' and '${triggersOverview}' screen items:
      |$style1 - (default) display hosts on the left side;
      |$style2 - display hosts on the top.
      |
      |Possible values for '${hostsInfo}' and ${triggersInfo} screen elements:
      |$style1 - (default) horizontal layout;
      |$style2 - vertical layout.
      |
      |Possible values for '${clock}' screen items:
      |$style1 - (default) local time;
      |$style2 - server time;
      |$style3 - host time.
      |
      |Possible values for '${plainText}' screen items:
      |$style1 - (default) display values as plain text;
      |$style2 - display values as HTML.
    """.stripMargin
  case object style1 extends ScreenItemStyle(0, "(default) style 1")
  case object style2 extends ScreenItemStyle(1, "style 2")
  case object style3 extends ScreenItemStyle(2, "style 3")
  case object unknown extends ScreenItemStyle(-1, "unknown")
}

case class ScreenItem[S <: EntityState](
  screenitemid:	EntityId = NotStoredId,
  resourcetype: ScreenResourceType,
  screenid:	EntityId = NotStoredId,
  application: Option[String],
  colspan: Option[IntProp],
  dynamic: Option[EnabledEnum],
  elements: Option[IntProp],
  halign: Option[HAlign],
  height: Option[IntProp],
  max_columns: Option[IntProp],
  //resourceid	string	ID of the object displayed on the screen item. Depending on the type of a screen item, the resourceid property can reference different objects.
  // Required for data overview, graph, map, plain text, screen, simple graph and trigger overview screen items. Unused by local and server time clocks, history of actions, history of events, hosts info, status of Zabbix, system status and URL screen items.
  resourceid: Option[EntityId],
  rowspan: Option[IntProp],
  sort_triggers: Option[SortTrigger],
  style: Option[ScreenItemStyle],
  url: Option[String],
  valign: Option[VAlign],
  width: Option[IntProp],
  x: Option[IntProp],
  y: Option[IntProp]
) extends Entity[S] {
  override protected[this] def id: EntityId = screenitemid

  def withScreenId[T >: S <: NotStored](screenId: EntityId): ScreenItem[NotStored] = {
    copy(screenid = screenId)
  }

  def toStored(id: StoredId): ScreenItem[Stored] = copy(screenid = id)

  def shouldBeUpdated[T >: S <: Stored](constant: ScreenItem[NotStored]): Boolean = {
    require(key == constant.key)

    resourcetype != constant.resourcetype ||
      shouldBeUpdated(application, constant.application) ||
      shouldBeUpdated(colspan, constant.colspan) ||
      shouldBeUpdated(dynamic, constant.dynamic) ||
      shouldBeUpdated(elements, constant.elements) ||
      shouldBeUpdated(halign, constant.halign) ||
      shouldBeUpdated(height, constant.height) ||
      shouldBeUpdated(max_columns, constant.max_columns) ||
      shouldBeUpdated(resourceid, constant.resourceid) ||
      shouldBeUpdated(rowspan, constant.rowspan) ||
      shouldBeUpdated(sort_triggers, constant.sort_triggers) ||
      shouldBeUpdated(style, constant.style) ||
      shouldBeUpdated(url, constant.url) ||
      shouldBeUpdated(valign, constant.valign) ||
      shouldBeUpdated(width, constant.width) ||
      shouldBeUpdated(x, constant.x) ||
      shouldBeUpdated(y, constant.y)
  }

  def key: ScreenItem.Key = (x.getOrElse(0), y.getOrElse(0))
}

object ScreenItem {
  type Key = (IntProp, IntProp)
  implicit val ordering: Ordering[Key] = Ordering.by(k => (k._1.value, k._2.value))

  implicit val format: Format[ScreenItem[Stored]] = Json.format[ScreenItem[Stored]]
  implicit val format2: Format[ScreenItem[NotStored]] = Json.format[ScreenItem[NotStored]]
}
