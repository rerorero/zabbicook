package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{StoredId, _}
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json._

/**
  * @see https://www.zabbix.com/documentation/3.0/manual/api/reference/template/object
  */
case class Template[S <: EntityState] (
  templateid: EntityId = NotStoredId,
  host: String,                   // required (Template name)
  description: Option[String]=None,  // optional
  name: Option[String]=None       // optional (Visible name)
) extends Entity[S] {

  override protected[this] val id: EntityId = templateid

  def toStored(id: StoredId): Template[Stored] = {
    copy(templateid = id).asInstanceOf[Template[Stored]]
  }

  def toJsonForUpdate[T >: S <: NotStored](_id: StoredId): JsObject = {
    Json.toJson(copy(templateid = _id).asInstanceOf[Template[Stored]]).as[JsObject]
  }

  /**
    * compare objects to check whether or not be updated
    * @return if it should be updated, returns true.
    */
  def shouldBeUpdated[T >: S <: Stored](constant: Template[NotStored]): Boolean = {
    require(host == constant.host)

    shouldBeUpdated(description, constant.description)
    shouldBeUpdated(name, constant.name)
  }
}

object Template {
  def fromString(hostname: String): Template[NotStored] = Template(host = hostname)

  implicit val format: Format[Template[Stored]] = Json.format[Template[Stored]]

  implicit val format2: Format[Template[NotStored]] = Json.format[Template[NotStored]]

  implicit val hocon: HoconReads[Template[NotStored]] = {
    for {
      host <- required[String]("name")
      description <- optional[String]("description")
      name <- optional[String]("visibleName")
    } yield {
      Template(
        host = host,
        description = description,
        name = name
      )
    }
  }
}
