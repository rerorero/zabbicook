package com.github.zabbicook.entity.graph

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.{EntityCompanionMetaHelper, IntProp}
import com.github.zabbicook.entity.prop.Meta._

case class GraphItemSetting(
  // gitemid is not required
  color: String,
  itemName: String,     // instead of itemid
                        // TODO: names are often duplicated, so it should be specified by 'key' as another ways.
  calc_fnc: Option[CalcFunction] = None,
  drawtype: Option[DrawType] = None,
  // graphid: EntityId = NotStoredId, // not handled
  sortorder: Option[IntProp] = None,
  `type`: Option[GraphItemType] = None,
  yaxisside: Option[YAxisSide] = None
) {
  def toGraphItem(itemId: StoredId): GraphItem[NotStored] = {
    GraphItem(NotStoredId, color, itemId, calc_fnc, drawtype, sortorder, `type`, yaxisside)
  }
}

object GraphItemSetting extends EntityCompanionMetaHelper {
  override val meta = entity("Graph item object.")(
    value("color")("color")("(required)	Graph item's draw color as a hexadecimal color code."),
    value("itemName")("itemName","item")("Name of the item associated with this graph item."),
    CalcFunction.meta("calc_fnc")("function"),
    DrawType.meta("drawtype")("style","drawStyle"),
    value("sortorder")("order")(s"""Position of the item in the graph.
                                    |Default: starts with 0 and increases by one with each entry.""".stripMargin),
    GraphItemType.meta("type")("type"),
    YAxisSide.meta("yaxisside")("yAxisSide","yAxis")
  ) _
}

case class GraphSetting(
  graph: Graph[NotStored],
  items: Seq[GraphItemSetting]
)

object GraphSetting extends EntityCompanionMetaHelper {
  override val meta = entity("Graph settings object.")(
    Graph.required("graph"),
    arrayOf("items")(GraphItemSetting.required("items"))
  ) _
}
