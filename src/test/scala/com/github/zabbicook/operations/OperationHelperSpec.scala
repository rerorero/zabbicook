package com.github.zabbicook.operations

import com.github.zabbicook.operation.OperationHelper
import com.github.zabbicook.test.UnitSpec
import play.api.libs.json.{JsString, Json}

class OperationHelperSpec extends UnitSpec with OperationHelper {

  "JsObject" should "be extended" in {
    val obj = Json.obj("first" -> 1, "second" -> "two")
      .prop("three" -> 3)
      .outExtend()
      .filter("id" -> Seq("a", "b"))

    assert(obj == Json.obj(
      "first" -> 1,
      "second" -> "two",
      "three" -> 3,
      "output" -> "extend",
      "filter" -> Json.obj(
        "id" -> Json.arr(JsString("a"), JsString("b"))
      )
    ))
  }
}
