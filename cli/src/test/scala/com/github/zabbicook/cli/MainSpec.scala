package com.github.zabbicook.cli

import com.github.zabbicook.test.UnitSpec

class MainSpec extends UnitSpec {

  def runMain(
    host: String,
    filePath: Option[String],
    debug: Boolean = false
  ): (Int, List[String]) = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[String]
    def mockedPrinter() = new Printer {
      override def print(msg: String): Unit = buf.append(msg)
    }

    val a = Seq("-a", host)
    val f = filePath.map(s => Seq("-f", s)).getOrElse(Seq())
    val d = if (debug) Seq("-d") else Seq()
    val printer = mockedPrinter()
    val code = await(new Main(printer).run(
      (a ++ f ++ d).toArray
    ))

    (code, buf.toList)
  }

  "CLI Main" should "parse and configure all from files" in {
    val path = getClass.getResource("/mainspec/zabbicook.conf").getPath()
    val (code, _) = runMain("http://localhost:8080/",Some(path))
    assert(code === 0)
  }
}
