package com.github.zabbicook.entity.user

import com.github.zabbicook.entity.trigger.Severity
import com.github.zabbicook.test.UnitSpec

class UserConfigSpec extends UnitSpec {

  "severitiesToBinary" should "convert to binaries" in {
    assert(12 === MediaConfig.severitiesToBinary(Seq(Severity.warning, Severity.average)))
  }
}
