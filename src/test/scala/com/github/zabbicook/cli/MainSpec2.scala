package com.github.zabbicook.cli

import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.operation.Ops
import com.github.zabbicook.test.{TestConfig, TestUsers, UnitSpec}
import play.api.libs.json.{JsObject, Json}

class MainSpec2 extends UnitSpec with MainSpecRunner with TestConfig with TestUsers {

  withTestApiConf { (conf, version) =>
    val ops = new Ops(new ZabbixApi(conf))

    def clean() = {
      cleanTestUsers(ops)
    }

    def run(oldPass: String, newPass: String): (Int, JsObject) = {
      val (code, ouput) = runMain(
        host = conf.apiPath,
        //debug = true,
        changePassOpts = Some((testUsers(0).user.alias, oldPass, newPass)),
        json = true
      )
      assert(1 === ouput.length)
      (code, Json.parse(ouput(0)).as[JsObject])
    }

    version + "CLI Main" should "change password" in {
      cleanRun(clean) {
        presentTestUsers(ops)

        {
          val (code, js) = run(testUsersPassword, "newPassword")
          assert(0 === code)
          assert("success" === (js \ "result").as[String])
          assert(1 === (js \ "report" \ "updated").as[Int])
          assert(1 === (js \ "report" \ "total").as[Int])
        }

        {
          val (code, js) = run(testUsersPassword, "newPassword")
          assert(0 === code)
          assert("success" === (js \ "result").as[String])
          assert(0 === (js \ "report" \ "updated").as[Int])
          assert(0 === (js \ "report" \ "total").as[Int])
        }

        {
          val (code, js) = run("hogehoge", "fugafuga")
          assert(0 !== code)
          assert("fail" === (js \ "result").as[String])
        }
      }
    }

    version + "CLI Main" should "report empty" in {
      val path = getClass.getResource("/empty/empty.conf").getPath()
      val (code, _) = runMain(conf.apiPath, Some(path), json = true)
      assert(0 === code)
    }
  }
}
