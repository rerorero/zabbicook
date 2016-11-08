package com.github.zabbicook.entity.graph

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop._
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, JsObject, Json}

sealed abstract class GraphType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object GraphType extends IntEnumPropCompanion[GraphType] {
  override val values: Set[GraphType] = Set(normal,stacked,pie,exploded,unknown)
  override val description: String = "Graph's layout type."
  case object normal extends GraphType(0,"(default) normal")
  case object stacked extends GraphType(1,("stacked"))
  case object pie extends GraphType(2, "pie")
  case object exploded extends GraphType(3, "exploded")
  case object unknown extends GraphType(-1, "unknown")
}

sealed abstract class GraphMinMaxType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object GraphMinMaxType extends IntEnumPropCompanion[GraphMinMaxType] {
  override val values: Set[GraphMinMaxType] = Set(calculated,fixed,unknown)
  override val description: String = "Maximum/Minimum value calculation method for the Y axis."
  case object calculated extends GraphMinMaxType(0, "(default) calculated")
  case object fixed extends GraphMinMaxType(1, "fixed")
  //case object item extends GraphMinMaxType(2, "item") // TODO item types are not supported yet
  case object unknown extends GraphMinMaxType(-1, "unknown")
}

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/graph/object
  */
case class Graph[S <: EntityState](
  graphid: EntityId = NotStoredId,
  height: IntProp,
  name: String,
  width: IntProp,
  //flags,    // not handled
  graphtype: Option[GraphType] = None,
  percent_left: Option[DoubleProp] = None,
  percent_right: Option[DoubleProp] = None,
  show_3d: Option[EnabledEnum] = None,
  show_legend: Option[EnabledEnum] = None,
  show_work_period: Option[EnabledEnum] = None,
  // templateid: EntityId = NotStoredId, not handled
  yaxismax: Option[DoubleProp] = None,
  yaxismin: Option[DoubleProp] = None,
  // ymax_itemid	// not handled string	ID of the item that is used as the maximum value for the Y axis. TODO support
  ymax_type: Option[GraphMinMaxType] = None,
  // ymin_itemid	// not handled string	ID of the item that is used as the minimum value for the Y axis. TODO support
  ymin_type: Option[GraphMinMaxType] = None
)extends Entity[S] {

  override protected[this] val id: EntityId = graphid

  def toStored(id: StoredId): Graph[Stored] = copy(graphid = id)

  def toJsonForUpdate[T >: S <: NotStored](_id: StoredId): JsObject = {
    Json.toJson(copy(graphid = _id).asInstanceOf[Graph[Stored]]).as[JsObject]
  }

  def shouldBeUpdated[T >: S <: Stored](constant: Graph[NotStored]): Boolean = {
    require(name == constant.name)
    height != constant.height ||
      width != constant.width ||
      shouldBeUpdated(graphtype, constant.graphtype) ||
      shouldBeUpdated(percent_left, constant.percent_left) ||
      shouldBeUpdated(percent_right, constant.percent_right) ||
      shouldBeUpdated(show_3d, constant.show_3d) ||
      shouldBeUpdated(show_legend, constant.show_legend) ||
      shouldBeUpdated(show_work_period, constant.show_work_period) ||
      shouldBeUpdated(yaxismax, constant.yaxismax) ||
      shouldBeUpdated(yaxismin, constant.yaxismin) ||
      shouldBeUpdated(ymax_type, constant.ymax_type) ||
      shouldBeUpdated(ymin_type, constant.ymin_type)
  }
}

object Graph extends EntityCompanionMetaHelper {

  implicit val format: Format[Graph[Stored]] = Json.format[Graph[Stored]]

  implicit val format2: Format[Graph[NotStored]] = Json.format[Graph[NotStored]]

  override val meta = entity("Graph object.")(
    readOnly("graphid"),
    value("height")("height")("(required)	Height of the graph in pixels."),
    value("name")("name")("(required)	Name of the graph."),
    value("width")("width")("(required)	Width of the graph in pixels."),
    GraphType.meta("graphtype")("type"),
    value("percent_left")("percentLeft","percentL")("""Left percentile.
                                                      |Default: 0.""".stripMargin),
    value("percent_right")("percentRight","percentR")("""Right percentile.
                                                        |Default: 0.""".stripMargin),
    EnabledEnum.metaWithDesc("show_3d")("view3D")("""Whether to show pie and exploded graphs in 3D.
                                                    |Default: false""".stripMargin),
    EnabledEnum.metaWithDesc("show_legend")("showLegend")("""Whether to show the legend on the graph.
                                                            |Default: true""".stripMargin),
    EnabledEnum.metaWithDesc("show_work_period")("showWorkingTime")("""Whether to show the working time on the graph.
                                                                      |Default:true""".stripMargin),
    value("yaxismax")("yAxisMax","yMax")("""The fixed maximum value for the Y axis.
                                           |Default: 100""".stripMargin),
    value("yaxismin")("yAxisMin","yMin")("""The fixed minimum value for the Y axis.
                                           |Default: 0""".stripMargin),
    GraphMinMaxType.meta("ymax_type")("yAxisMaxType","yMaxType"),
    GraphMinMaxType.meta("ymin_type")("yAxisMinType","yMinType")
  ) _
}
