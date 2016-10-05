package com.github.zabbicook.entity.prop

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

case class IntProp(value: Int)

/**
  * zabbix api returns strings as numbers.
  * NumProp can parse number types and number string types.
  */
object IntProp {
  implicit def numToNumProp(n: Int): IntProp = IntProp(n)
  implicit def numToNumProp(n: Option[Int]): Option[IntProp] = n.map(numToNumProp)

  implicit val format: Format[IntProp] = Format(
    Reads[IntProp] {
      case JsNumber(n) => JsSuccess(IntProp(n.toInt))
      case JsString("") => JsSuccess(null) // NOTICE!! The property which does not defines a default value, respond empty string and set null! OMG!!
      case JsString(s) => Try(s.toInt) match {
        case Success(n) => JsSuccess(IntProp(n))
        case Failure(e) => JsError(s"expected number or number string: $s")
      }
      case els => JsError(s"expected number or number string but: $els")
    },
    Writes(v => JsNumber(v.value))
  )
}
