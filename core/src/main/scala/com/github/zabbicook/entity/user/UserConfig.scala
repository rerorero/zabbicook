package com.github.zabbicook.entity.user

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.media.Media
import com.github.zabbicook.entity.prop.{EnabledEnumZeroPositive, EntityCompanionMetaHelper}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.trigger.Severity

case class MediaConfig(
  active: EnabledEnumZeroPositive,
  mediaType: String,
  period: String,
  sendto: String,
  severity: Seq[Severity]
) {
  def toMedia(mediaTypeId: StoredId): Media[NotStored] = {
    Media(NotStoredId, active, mediaTypeId, period, sendto, MediaConfig.severitiesToBinary(severity))
  }
}

object MediaConfig extends EntityCompanionMetaHelper {

  val meta = entity("The Media object")(
    EnabledEnumZeroPositive.metaWithDesc("active")("enabled")("(required)	Whether the media is enabled."),
    value("mediaType")("type")("(required) The media type used by the media."),
    value("period")("whenActive","period")("""(required) Time when the notifications can be sent as a time period.
                                             |ex. "1-7,00:00-24:00" """.stripMargin),
    value("sendto")("sendTo")("(required) Address, user name or other identifier of the recipient."),
    arrayOf("severity")(
      Severity.metaWithDesc("severity")("severity")(
      """(required)	Trigger severities to send notifications about.
        |Notifications will be sent from triggers with severities as followed this setting.""".stripMargin))
  ) _

  def severitiesToBinary(severities: Seq[Severity]): Int = {
    severities.foldLeft(0) { (acc, s) =>
      acc + (1 << (s.zabbixValue.value))
    }
  }
}

/**
  * @param user user object
  * @param groupNames names of groups to which the user belongs
  * @param password password
  * @param initialPassword If true, password is used only when the user is created.
  * @param media array of usermedia
  */
case class UserConfig(
  user: User[NotStored],
  groupNames: Seq[String],
  password: String,
  initialPassword: Boolean,
  media: Option[Seq[MediaConfig]]
)

object UserConfig extends EntityCompanionMetaHelper {
  val meta = entity("User settings.")(
    User.required("user"),
    array("groupNames")("groups")("(required) User groups to add the user to."),
    value("password")("password")("(required) User's password."),
    value("initialPassword")("initialPassword")("""(required) Whether the password is used only when the user is created.
                                                    |If set false, zabbicook always set the password.""".stripMargin),
    arrayOf("media")(MediaConfig.optional("media"))
  ) _
}
