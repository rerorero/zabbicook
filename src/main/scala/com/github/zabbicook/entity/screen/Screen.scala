package com.github.zabbicook.entity.screen

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop.{EntityCompanionMetaHelper, IntProp}
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

case class Screen[S <: EntityState](
  screenid: EntityId = NotStoredId,
  name: String,
  hsize: Option[IntProp],
  vsize: Option[IntProp]
  // userid: EntityId = NotStoredId,  // TODO: implement
  //`private`: Option[EnabledEnum]    // TODO: implement
) extends  Entity[S] {
    override protected[this] def id: EntityId = screenid

    def toStored(id: StoredId): Screen[Stored] = copy(screenid = id)

  def shouldBeUpdated[T >: S <: Stored](constant: Screen[NotStored]): Boolean = {
    require(name == constant.name)

    shouldBeUpdated(hsize, constant.hsize) ||
      shouldBeUpdated(vsize, constant.vsize)
  }
}

object Screen extends EntityCompanionMetaHelper {
  implicit val format: Format[Screen[Stored]] = Json.format[Screen[Stored]]
  implicit val format2: Format[Screen[NotStored]] = Json.format[Screen[NotStored]]

  override val meta = entity("The screen object.")(
    readOnly("screenid"),
    value("name")("name")("(required) Name of the screen."),
    value("hsize")("columns","width")("Width of the screen. Default: 1."),
    value("vsize")("rows","height")("Height of the screen. Default: 1.")
    // readOnly("userid"),
//    EnabledEnum.metaWithDesc("private")("private")(
//      """Type of screen sharing.
//        |Screen is public if set false.""".stripMargin)
  ) _
}

