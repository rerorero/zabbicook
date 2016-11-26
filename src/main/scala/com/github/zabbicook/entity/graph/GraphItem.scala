package com.github.zabbicook.entity.graph

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.{EnumProp, IntEnumPropCompanion, IntProp}
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, JsObject, Json}

sealed abstract class CalcFunction(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object CalcFunction extends IntEnumPropCompanion[CalcFunction] {
  override val values: Seq[CalcFunction] = Seq(minimum,average,maximum,all,last,unknown)
  override val description: String = "Value of the item that will be displayed."
  case object minimum extends CalcFunction(1, "minimum value")
  case object average extends CalcFunction(2, "(default) average value")
  case object maximum extends CalcFunction(4, "maximum value")
  case object all extends CalcFunction(7, "all values")
  case object last extends CalcFunction(9, "last value, used only by pie and exploded graphs")
  case object unknown extends CalcFunction(-1, "unknown")
}

sealed abstract class DrawType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object DrawType extends IntEnumPropCompanion[DrawType] {
  override val values: Seq[DrawType] = Seq(default,region,bold,dot,dashed,gradient,unknown)
  override val description: String = "Draw style of the graph item."
  case object default extends DrawType(0, "(defualt) line")
  case object region extends DrawType(1, "filled region")
  case object bold extends DrawType(2, "bold line")
  case object dot extends DrawType(3, "dot")
  case object dashed extends DrawType(4, "dashed line")
  case object gradient extends DrawType(5, "gradient line")
  case object unknown extends DrawType(-1, "unknown")
}

sealed abstract class GraphItemType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object GraphItemType extends IntEnumPropCompanion[GraphItemType] {
  override val values: Seq[GraphItemType] = Seq(simple,sum,unknown)
  override val description: String = "Type of graph item."
  case object simple extends GraphItemType(0, "(default) simple")
  case object sum extends GraphItemType(2, "graph sum, used only by pie and exploded graphs.")
  case object unknown extends GraphItemType(-1, "unknown")
}

sealed abstract class YAxisSide(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object YAxisSide extends IntEnumPropCompanion[YAxisSide] {
  override val values: Seq[YAxisSide] = Seq(left,right,unknown)
  override val description: String = "Side of the graph where the graph item's Y scale will be drawn."
  case object left extends YAxisSide(0, "(default) left side")
  case object right extends YAxisSide(1, "right side")
  case object unknown extends YAxisSide(-1, "unknown")
}

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/graphitem/object
  */
case class GraphItem[S <: EntityState](
  gitemid: EntityId = NotStoredId,
  color: String,
  itemid: EntityId = NotStoredId,
  calc_fnc: Option[CalcFunction] = None,
  drawtype: Option[DrawType] = None,
  // graphid: EntityId = NotStoredId, // not handled
  sortorder: Option[IntProp] = None,
  `type`: Option[GraphItemType] = None,
  yaxisside: Option[YAxisSide] = None
) extends Entity[S] {

  override protected[this] val id: EntityId = gitemid

  def toStored(_gitemId: StoredId, _itemId: StoredId): GraphItem[Stored] = copy(gitemid = id, itemid = _itemId)

  def toJsonForUpdate[T >: S <: NotStored](_id: StoredId): JsObject = {
    Json.toJson(copy(gitemid = _id).asInstanceOf[GraphItem[Stored]]).as[JsObject]
  }

  def shouldBeUpdated[T >: S <: Stored](constant: GraphItem[NotStored]): Boolean = {
    color != constant.color ||
      shouldBeUpdated(calc_fnc, constant.calc_fnc) ||
      shouldBeUpdated(drawtype, constant.drawtype) ||
      shouldBeUpdated(sortorder, constant.sortorder) ||
      shouldBeUpdated(`type`, constant.`type`) ||
      shouldBeUpdated(yaxisside, constant.yaxisside)
  }
}

object GraphItem {
  implicit val format: Format[GraphItem[Stored]] = Json.format[GraphItem[Stored]]

  implicit val format2: Format[GraphItem[NotStored]] = Json.format[GraphItem[NotStored]]
}
