package com.github.zabbicook.entity.action

import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.prop._
import com.github.zabbicook.entity.{EntityId, PropCompare}
import play.api.libs.json.{Format, Json}

case class OperationMessage(
  default_msg: Option[EnabledEnum],
  mediatypeid: Option[EntityId] = None,
  message: Option[String],
  subject: Option[String]
) extends PropCompare {

  def toStored(mediatypeId: Option[StoredId]): OperationMessage= {
    copy(mediatypeid = mediatypeId.flatMap {
      case s if s.id.isEmpty => None
      case s => Some(s)
    })
  }

  def isMediaTypeIdSet: Boolean = {
    mediatypeid match {
      case Some(StoredId(_)) => true
      case Some(_) => false
      case None => true // omitted
    }
  }

  def isSame(constant: OperationMessage): Boolean = {
    isSameProp(default_msg, constant.default_msg, EnabledEnum.disabled) &&
    mediatypeid == constant.mediatypeid &&
    isSameProp(message, constant.message, "") &&
    isSameProp(subject, constant.subject, "")
  }
}


object OperationMessage {
  implicit val format: Format[OperationMessage] = Json.format[OperationMessage]

  val default: OperationMessage = OperationMessage(None,None,None,None)
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
