package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop._
import play.api.libs.json.{Format, JsObject, Json}

sealed abstract class Theme(val zabbixValue: String, val desc: String) extends EnumProp[String]

object Theme extends StringEnumPropCompanion[Theme] {
  override val values: Set[Theme] = Set(default,blue,dark,unknown)
  override val description: String = "User's theme."
  case object default extends Theme("default", "default")
  case object blue  extends Theme("blue-theme", "Blue")
  case object dark  extends Theme("dark-theme", "Dark")
  case object unknown extends Theme("unknown", "Unknown")
}

sealed abstract class UserType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object UserType extends IntEnumPropCompanion[UserType] {
  override val values: Set[UserType] = Set(user,admin,superAdmin,unknown)
  override val description: String = "Type of the user."
  case object user extends UserType(1, "(default) Zabbix user")
  case object admin extends UserType(2, "Zabbix admin")
  case object superAdmin extends UserType(3, "Zabbix super admin")
  case object unknown extends UserType(-1, "Unknown")
}

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/user/object
  */
case class User[S <: EntityState](
  userid: EntityId = NotStoredId, // read only
  alias: String,          // required
//  attempt_clock: Option[String], // unhandled
//  attempt_failed: Option[String], // unhandled
//  attempt_ip: Option[String], // unhandled
  autologin: Option[EnabledEnum] = None,
  //autologout: Option[IntProp] = None, // unhandled
  lang: Option[String] = None,
  name: Option[String] = None,
  refresh: Option[IntProp] = None,
  rows_per_page: Option[IntProp] = None,
  surname: Option[String] = None,
  theme: Option[Theme] = None,
  `type`: Option[UserType] = None,
  url: Option[String] = None
) extends Entity[S] {

  override protected[this] val id: EntityId = userid

  def toStored(id: StoredId): User[Stored] = copy(userid = id)

  def toJsonForUpdate[T >: S <: NotStored](_id: StoredId): JsObject = {
    Json.toJson(copy(userid = _id).asInstanceOf[User[Stored]]).as[JsObject]
  }

  /**
    * compare object to check whether or not you need to update the stored object
    * @param constant with which to compare(requires same alias.)
    * @return true: need to update stored entity
    *         false: There is no differences.
    */
  def shouldBeUpdated[T >: S <: Stored](constant: User[NotStored]): Boolean = {
    require(alias == constant.alias)
    shouldBeUpdated(autologin, constant.autologin) ||
    shouldBeUpdated(lang, constant.lang) ||
    shouldBeUpdated(name, constant.name) ||
    shouldBeUpdated(refresh, constant.refresh) ||
    shouldBeUpdated(rows_per_page, constant.rows_per_page) ||
    shouldBeUpdated(surname, constant.surname) ||
    shouldBeUpdated(theme, constant.theme) ||
    shouldBeUpdated(url, constant.url)
  }
}

object User extends EntityCompanionMetaHelper {
  implicit val format: Format[User[Stored]] = Json.format[User[Stored]]

  implicit val format2: Format[User[NotStored]] = Json.format[User[NotStored]]

  override val meta = entity("User object.")(
    readOnly("userid"),
    value("alias")("alias")("(required) User alias."),
    EnabledEnum.metaWithDesc("autologin")("autoLogin","autologin")("Whether to enable auto-login."),
    value("lang")("lang","language")(""" Language code of the user's language.
                                     |Default: en_GB.""".stripMargin),
    value("name")("name")("Name of the user."),
    value("refresh")("refresh")("""Automatic refresh period in seconds.
                                  |Default: 30.""".stripMargin),
    value("rows_per_page")("rowsPerPage","rows")("""Amount of object rows to show per page.
                                                   |Default: 50.""".stripMargin),
    value("surname")("surName","surname","sur")("Surname of the user."),
    Theme.meta("theme")("theme"),
    UserType.meta("type")("type"),
    value("url")("url")("URL of the page to redirect the user to after logging in.")
  ) _
}
