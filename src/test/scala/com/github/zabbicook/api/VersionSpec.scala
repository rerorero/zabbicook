package com.github.zabbicook.api

import com.github.zabbicook.test.UnitSpec

class VersionSpec extends UnitSpec {
  "Version" should "be compared" in {
    assert(Version(1, 2, 3) > Version(1, 2, 2))
    assert(Version(1, 2, 3) == Version(1, 2, 3))
    assert(Version(1, 2, 3) < Version(2, 1, 1))
  }
}
