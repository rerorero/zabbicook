package com.github.zabbicook.entity

import com.github.zabbicook.entity.Template.TemplateId
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{Format, Json}

/**
  * @see https://www.zabbix.com/documentation/3.0/manual/api/reference/template/object
  */
case class Template (
  templateid: Option[TemplateId] = None,  // read only
  host: String,                   // required (Template name)
  description: Option[String]=None,  // optional
  name: Option[String]=None       // optional (Visible name)
) extends Entity {
  def removeReadOnly: Template = copy(templateid = None)

  /**
    * compare objects to check wheter or not be updated
    * @return if it should be updated, returns true.
    */
  def shouldBeUpdated(constant: Template): Boolean = {
    require(host == constant.host)
    shouldBeUpdated(description, constant.description)
    shouldBeUpdated(name, constant.name)
  }
}

object Template {
  type TemplateId = String

  def fromString(hostname: String): Template = Template(host = hostname)

  implicit val format: Format[Template] = Json.format[Template]

  implicit val hocon: HoconReads[Template] = {
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
