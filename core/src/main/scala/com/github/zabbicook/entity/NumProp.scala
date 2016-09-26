package com.github.zabbicook.entity

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

case class NumProp(value: Int)

/**
  * zabbix api returns strings as numbers.
  * NumProp can parse number types and number string types.
  */
object NumProp {
  implicit def numToNumProp(n: Int): NumProp = NumProp(n)
  implicit def numToNumProp(n: Option[Int]): Option[NumProp] = n.map(numToNumProp)

  implicit val format: Format[NumProp] = Format(
    Reads[NumProp] {
      case JsNumber(n) => JsSuccess(NumProp(n.toInt))
      case JsString(s) => Try(s.toInt) match {
        case Success(n) => JsSuccess(NumProp(n))
        case Failure(e) => JsError(s"expected number or number string: $s")
      }
      case els => JsError(s"expected number or number string but: $els")
    },
    Writes(v => JsNumber(v.value))
  )
}
