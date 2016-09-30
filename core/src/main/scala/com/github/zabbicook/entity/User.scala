package com.github.zabbicook.entity

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.User.AutoLogin
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{Format, JsObject, Json}

sealed abstract class Theme(val value: String) extends StringEnumProp {
  override def validate(): ValidationResult = Theme.validate(this)
}

object Theme extends StringEnumCompanion[Theme] {
  override val all: Set[Theme] = Set(default,blue,dark,unknown)
  case object default extends Theme("default")
  case object blue  extends Theme("blue-theme")
  case object dark  extends Theme("dark-theme")
  case object unknown extends Theme("unknown")
}

sealed abstract class UserType(val value: NumProp) extends NumberEnumDescribedWithString {
  override def validate(): ValidationResult = UserType.validate(this)
}

object UserType extends NumberEnumDescribedWithStringCompanion[UserType] {
  override val all: Set[UserType] = Set(user,admin,superAdmin,unknown)
  case object user extends UserType(1)
  case object admin extends UserType(2)
  case object superAdmin extends UserType(3)
  case object unknown extends UserType(-1)
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
  autologin: Option[AutoLogin] = None,
  autologout: Option[NumProp] = None,
  lang: Option[String] = None,
  name: Option[String] = None,
  refresh: Option[NumProp] = None,
  rows_per_page: Option[NumProp] = None,
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
    shouldBeUpdated(autologout, constant.autologout) ||
    shouldBeUpdated(lang, constant.lang) ||
    shouldBeUpdated(name, constant.name) ||
    shouldBeUpdated(refresh, constant.refresh) ||
    shouldBeUpdated(rows_per_page, constant.rows_per_page) ||
    shouldBeUpdated(surname, constant.surname) ||
    shouldBeUpdated(theme, constant.theme) ||
    shouldBeUpdated(url, constant.url)
  }
}

object User {
  type AutoLogin = EnabledEnum

  implicit val format: Format[User[Stored]] = Json.format[User[Stored]]

  implicit val format2: Format[User[NotStored]] = Json.format[User[NotStored]]

  implicit val hocon: HoconReads[User[NotStored]] = {
    for {
      alias <- required[String]("alias")
      autologin <- optional[EnabledEnum]("autoLogin")
      // TODO Can autologout be set ?
      // autologout <- optional[Int]("autoLogout")
      lang <- optional[String]("lang")
      name <- optional[String]("name")
      surName <- optional[String]("surName")
      refresh <- optional[Int]("refresh")
      rowsPerPage <- optional[Int]("rowsPerPage")
      theme <- optional[Theme]("theme")
      userType <- optional[UserType]("type")
      url <- optional[String]("url")
    } yield {
      User(
        alias = alias,
        autologin = autologin,
        // autologout = autologout,
        lang = lang,
        name = name,
        refresh = refresh,
        rows_per_page = rowsPerPage,
        surname = surName,
        theme = theme,
        `type` = userType,
        url = url
      )
    }
  }
}
