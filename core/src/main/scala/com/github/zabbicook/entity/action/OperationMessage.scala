package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.EntityId
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.prop._
import play.api.libs.json.{Format, Json}

case class OperationMessage(
  default_msg: Option[EnabledEnumZeroPositive],
  mediatypeid: Option[EntityId] = None,
  message: Option[String],
  subject: Option[String]
) {
  def toStored(mediatypeId: Option[StoredId]): OperationMessage= {
    copy(mediatypeid = mediatypeId.flatMap {
      case s if s.id.isEmpty => None
      case s => Some(s)
    })
  }
}


object OperationMessage {
  implicit val format: Format[OperationMessage] = Json.format[OperationMessage]
}

case class OpMessageGroup(
  usrgrpid: EntityId
)

object OpMessageGroup {
  implicit val format: Format[OpMessageGroup] = Json.format[OpMessageGroup]
}

case class OpMessageUser(
  userid: EntityId
)

object OpMessageUser {
  implicit val format: Format[OpMessageUser] = Json.format[OpMessageUser]
}
