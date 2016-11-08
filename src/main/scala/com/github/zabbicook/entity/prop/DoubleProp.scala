package com.github.zabbicook.entity.prop

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

case class DoubleProp(value: Double)

/**
  * zabbix api returns strings as numbers.
  * NumProp can parse number types and number string types.
  */
object DoubleProp {
  implicit def numToNumProp(n: Double): DoubleProp = DoubleProp(n)
  implicit def numToNumProp(n: Option[Double]): Option[DoubleProp] = n.map(numToNumProp)

  implicit val format: Format[DoubleProp] = Format(
    Reads[DoubleProp] {
      case JsNumber(n) => JsSuccess(DoubleProp(n.toDouble))
      case JsString(s) => Try(s.toDouble) match {
        case Success(n) => JsSuccess(DoubleProp(n))
        case Failure(e) => JsError(s"expected number or number string: $s")
      }
      case els => JsError(s"expected number or number string but: $els")
    },
    Writes(v => JsNumber(v.value))
  )
}
