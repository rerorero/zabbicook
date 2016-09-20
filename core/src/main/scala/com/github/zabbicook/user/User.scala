package com.github.zabbicook.user

import com.github.zabbicook.entity._
import com.github.zabbicook.user.User.{AutoLogin, UserId}
import play.api.libs.json.{Format, Json}

sealed abstract class Theme(val value: String) extends StringEnumProp

object Theme extends StringEnumCompanion[Theme] {
  override val all: Set[Theme] = Set(default,classic,originalBlue,darkBlue,darkOrange,unknown)
  case object default extends Theme("default")
  case object classic extends Theme("classic")
  case object originalBlue extends Theme("originalblue")
  case object darkBlue extends Theme("darkblue")
  case object darkOrange  extends Theme("darkorange")
  case object unknown extends Theme("unknown")
}

sealed abstract class UserType(val value: NumProp) extends NumberEnumProp

object UserType extends NumberEnumPropCompanion[UserType] {
  override val all: Set[UserType] = Set(user,admin,superAdmin,unknown)
  case object user extends UserType(1)
  case object admin extends UserType(2)
  case object superAdmin extends UserType(3)
  case object unknown extends UserType(-1)
}

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/user/object
  */
case class User(
  userid: Option[UserId] = None, // read only
  alias: String,          // required
//  attempt_clock: Option[String], // unhandled
//  attempt_failed: Option[String], // unhandled
//  attempt_ip: Option[String], // unhandled
  autologin: Option[AutoLogin] = None,
  autologout: Option[NumProp] = None,
  lang: Option[String] = None,
  name: Option[String] = None,
  refresh: Option[NumProp] = None,
  rows_per_page: Option[NumProp] = None,
  sername: Option[String] = None,
  theme: Option[Theme] = None,
  url: Option[String] = None
) extends Entity {
  def removeReadOnly: User = copy(userid = None)

  /**
    * compare object to check whether or not you need to update the stored object
    * @param constant with which to compare(requires same alias.)
    * @return true: need to update stored entity
    *         false: There is no differences.
    */
  def shouldBeUpdated(constant: User): Boolean = {
    require(alias == constant.alias)
    shouldBeUpdated(autologin, constant.autologin) ||
    shouldBeUpdated(autologout, constant.autologout) ||
    shouldBeUpdated(lang, constant.lang) ||
    shouldBeUpdated(name, constant.name) ||
    shouldBeUpdated(refresh, constant.refresh) ||
    shouldBeUpdated(rows_per_page, constant.rows_per_page) ||
    shouldBeUpdated(sername, constant.sername) ||
    shouldBeUpdated(theme, constant.theme) ||
    shouldBeUpdated(url, constant.url)
  }
}

object User {
  type UserId = String
  type AutoLogin = EnabledEnum
  implicit val format: Format[User] = Json.format[User]
}
