package com.github.zabbicook.cli

import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.operation.Ops
import com.github.zabbicook.test.{TestConfig, TestUsers, UnitSpec}

class MainSpec2 extends UnitSpec with MainSpecRunner with TestConfig with TestUsers {

  withTestApiConf { (conf, version) =>
    val ops = new Ops(new ZabbixApi(conf))

    def clean() = {
      cleanTestUsers(ops)
    }

    def run(oldPass: String, newPass: String): (Int, List[String]) = {
      runMain(
        host = conf.apiPath,
        //debug = true,
        changePassOpts = Some((testUsers(0).user.alias, oldPass, newPass))
      )
    }

    version + "CLI Main" should "change password" in {
      cleanRun(clean) {
        presentTestUsers(ops)

        {
          val (code, output) = run(testUsersPassword, "newPassword")
          assert(0 === code)
          assert(0 == output.length)
        }

        {
          val (code, output) = run(testUsersPassword, "newPassword")
          assert(0 === code)
          assert(0 == output.length)
        }

        {
          val (code, output) = run("hogehoge", "fugafuga")
          assert(0 !== code)
        }
      }
    }
  }
}
