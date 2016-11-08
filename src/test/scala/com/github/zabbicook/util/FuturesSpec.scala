package com.github.zabbicook.util

import java.util.concurrent.atomic.AtomicInteger

import com.github.zabbicook.test.UnitSpec

import scala.concurrent.Future

class FuturesSpec extends UnitSpec {

  "sequential" should "run sequentialy" in {
    val times = Seq(
      1, 3, 2, 4, 0
    )
    val atomicInteger = new AtomicInteger(0)
    def wait(time: Int): Future[Int] = Future {
      val i = times.indexOf(time)
      assert(atomicInteger.get() === i)
      Thread.sleep(time * 100)
      atomicInteger.incrementAndGet()
      time
    }
    val actual = await(Futures.sequential(times)(wait))
    assert(times === actual)
  }
}
