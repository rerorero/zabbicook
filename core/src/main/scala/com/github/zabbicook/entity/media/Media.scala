package com.github.zabbicook.entity.media

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.prop.Meta._
import com.github.zabbicook.entity.prop.{EnabledEnumZeroPositive, EntityCompanionMetaHelper, IntProp}
import com.github.zabbicook.entity.trigger.Severity
import com.github.zabbicook.entity.{Entity, EntityId, EntityState}
import play.api.libs.json.{Format, Json}

case class Media[S <: EntityState](
  mediaid: EntityId,
  active: EnabledEnumZeroPositive,
  mediatypeid: EntityId,
  period: String,
  sendto: String,
  severity: IntProp, // TODO converted.
  userid: EntityId
) extends Entity[S] {
  override protected[this] def id: EntityId = mediaid
}

object Media extends EntityCompanionMetaHelper {
  implicit val format: Format[Media[NotStored]] = Json.format[Media[NotStored]]

  implicit val format2: Format[Media[Stored]] = Json.format[Media[Stored]]

  val meta = entity("The Media object")(
    readOnly("mediaid"),
    EnabledEnumZeroPositive.metaWithDesc("active")("enabled")("(required)	Whether the media is enabled."),
    readOnly("mediatypeid"),
    value("period")("whenActive","period")("""(required) Time when the notifications can be sent as a time period.
                                             |ex. "1-7,00:00-24:00" """.stripMargin),
    value("sendto")("sendTo")("(required) Address, user name or other identifier of the recipient."),
    value("severity")("severity","useIfSeverity")(
      """(required)	Trigger severities to send notifications about.
        |Severities are stored in binary form with each bit representing the corresponding severity. For example, 12 equals 1100 in binary and means, that notifications will be sent from triggers with severities warning and average. """.stripMargin),
    readOnly("userid")
  ) _

  def severitiesToBinary(severities: Seq[Severity]): Int = {
    severities.foldLeft(0) { (acc, s) =>
      acc + (1 << (s.zabbixValue.value - 1))
    }
  }
}

