package com.github.zabbicook.entity.media

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop._
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, JsObject, Json}

sealed abstract class MediaTypeType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object MediaTypeType extends IntEnumPropCompanion[MediaTypeType] {
  override val values: Set[MediaTypeType] = Set(email,script,SMS,Jabber,EzTexting,unknown)
  override val description: String = "(required) Transport used by the media type."
  case object email extends MediaTypeType(0, "email")
  case object script extends MediaTypeType(1, "script")
  case object SMS extends MediaTypeType(2, "SMS")
  case object Jabber extends MediaTypeType(3, "Jabber")
  case object EzTexting extends MediaTypeType(100, "Ez Texting")
  case object unknown extends MediaTypeType(-1, "unknown")
}

case class MediaType[S <: EntityState](
  mediatypeid: EntityId = NotStoredId,
  description: String,
  `type`: MediaTypeType,
  exec_path: Option[String] = None,
  gsm_modem: Option[String] = None,
  passwd: Option[String] = None,
  smtp_email: Option[String] = None,
  smtp_helo: Option[String] = None,
  smtp_server: Option[String] = None,
  status: EnabledEnumZeroPositive,
  username: Option[String] = None,
  exec_params: Option[String] = None
) extends Entity[S] {
  override protected[this] def id: EntityId = mediatypeid

  def toStored(id: StoredId): MediaType[Stored] = copy(mediatypeid = id)

  def shouldBeUpdated[T >: S <: Stored](constant: MediaType[NotStored]): Boolean = {
    require(description == constant.description)
    `type` != constant.`type` ||
      shouldBeUpdated(exec_path, constant.exec_path) ||
      shouldBeUpdated(gsm_modem, constant.gsm_modem) ||
      shouldBeUpdated(passwd, constant.passwd) ||
      shouldBeUpdated(smtp_email, constant.smtp_email) ||
      shouldBeUpdated(smtp_helo, constant.smtp_helo) ||
      shouldBeUpdated(smtp_server, constant.smtp_server) ||
      status != constant.status ||
      shouldBeUpdated(username, constant.username) ||
      shouldBeUpdated(exec_params, constant.exec_params)
  }

  def toJsonForUpdate[T >: S <: NotStored](_id: StoredId): JsObject = {
    Json.toJson(copy(mediatypeid = _id).asInstanceOf[MediaType[Stored]]).as[JsObject]
  }
}

object MediaType extends EntityCompanionMetaHelper {
  implicit val format: Format[MediaType[NotStored]] = Json.format[MediaType[NotStored]]

  implicit val format2: Format[MediaType[Stored]] = Json.format[MediaType[Stored]]

  val meta = entity("The MediaType object")(
    readOnly("mediatypeid"),
    value("description")("name")("(required)	Name of the media type."),
    MediaTypeType.meta("type")("type"),
    value("exec_path")("scriptName","textLimit")(
      """For script media types exec_path contains the name of the executed script.
        |For Ez Texting exec_path contains the message text limit.
        |  Possible text limit values:
        |  0 - USA (160 characters);
        |  1 - Canada (136 characters).
        |Required for script and Ez Texting media types. """.stripMargin),
    value("gsm_modem")("gsmModem")(
      """Serial device name of the GSM modem.
        |Required for SMS media types. """.stripMargin),
    value("passwd")("password")(
      """Authentication password.
        |Required for Jabber and Ez Texting media types. """.stripMargin),
    value("smtp_email")("SMTPEmail")(
      """Email address from which notifications will be sent.
        |Required for email media types. """.stripMargin),
    value("smtp_helo")("SMTPHELO")(
      """SMTP HELO.
        |Required for email media types. """.stripMargin),
    value("smtp_server")("SMTPServer")(
      """SMTP server.
        |Required for email media types. """.stripMargin),
    EnabledEnumZeroPositive.metaWithDesc("status")("enabled")("Whether the media type is enabled."),
    value("username")("user","username")(
      """Username or Jabber identifier.
        |Required for Jabber and Ez Texting media types. """.stripMargin),
    value("exec_params")("scriptParameters","params")(
      """Script parameters.
        |Each parameter ends with a new line feed. """.stripMargin)
  ) _
}
