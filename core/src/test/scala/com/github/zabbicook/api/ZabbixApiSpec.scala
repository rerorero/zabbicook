package com.github.zabbicook.api

import com.github.zabbicook.operation.OperationHelper
import com.github.zabbicook.test.{TestConfig, UnitSpec}
import play.api.libs.json.{JsArray, JsObject, JsString, Json}

import scala.concurrent.Future

class ZabbixApiSpec extends UnitSpec with OperationHelper with TestConfig {
  lazy val sut = new ZabbixApi(apiConf)

  "ZabbixApi.request" should "returns version info" in {
    whenReady(sut.request("apiinfo.version", JsObject(Seq()), auth = false)) { r =>
      assert(r.as[String].startsWith("3")) // TODO how about comptible with 2.x?
    }
  }

  "ZabbixApi.request" should "authorize" in {
    val q = Json.obj()
      .outExtend()
      .filter("alias" -> apiConf.authUser)

    whenReady(sut.request("user.get", q)) { r =>
      assert(
        r.as[JsArray].value.head
          .as[JsObject].value
          .get("alias") === Some(JsString(apiConf.authUser))
      )
    }
  }

  "ZabbixApi.requestSingle" should "return single result" in {
    val q = Json.obj()
      .outExtend()
      .filter("alias" -> apiConf.authUser)
    whenReady(sut.requestSingle("user.get", q)) {
      case Some(r) =>
        assert(
          r.as[JsObject].value
            .get("alias") === Some(JsString(apiConf.authUser))
        )
      case None => fail()
    }
  }

  "ZabbixApi.requestSingle" should "return none if not exists" in {
    val q = Json.obj()
      .outExtend()
      .filter("alias" -> "invalid user alias")
    whenReady(sut.requestSingle("user.get", q))(r => assert(r == None))
  }

  "ZabbixApi.requestSingle" should "throws an exception if duplicated" in {
    val usrgrpid = "7" // TODO to replace fixed value
    val aliases = Seq(
      specName("api1"),
      specName("api2")
    )

    def clean(): Unit = aliases foreach { alias =>
      val fut = sut.requestSingle("user.get", Json.obj().filter("alias" -> alias)).flatMap {
        case Some(js) =>
          sut.request("user.delete", Json.arr((js \ "userid").as[String]))
        case None => Future(Unit)
      }
      await(fut)
    }

    cleanRun(clean) {
      aliases foreach { alias =>
        val param = Json.obj()
          .prop("alias" -> alias)
          .prop("passwd" -> s"Doe123")
          .prop("name" -> "test")
          .prop("usrgrps" -> Json.obj("usrgrpid" -> usrgrpid))
        await(sut.request("user.create", param))
      }
      val q = Json.obj()
        .outExtend()
        .filter("name" -> "test")
      intercept[NotSingleException] {
        await(sut.requestSingle("user.get", q))
      }
    }
  }
}
