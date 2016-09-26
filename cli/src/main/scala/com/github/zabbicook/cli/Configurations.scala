package com.github.zabbicook.cli

import java.io.File

case class Configurations(
  input: File = new File("zabbicook.conf"),
  url: String = "http://localhost/",
  adminUser: String = "Admin",
  adminPass: String = "zabbix",
  isJson: Boolean = false,
  isDebug: Boolean = false
)

object Configurations {
  val parser = new scopt.OptionParser[Configurations]("zabbicook") {
    head("zabbicook")

    opt[File]('f', "file").required()
      .valueName("<file>")
      .action((x, c) => c.copy(input = x))
      .text("input is a required file by formatting Hocon (default is 'zabbicook.conf')")

    opt[String]('a', "api").optional()
      .valueName("<url>")
      .action((x, c) => c.copy(url = x))
      .text("Endpoint URL of zabbix http server (default is 'http://localhost/')")

    opt[String]('u', "user").optional()
      .valueName("<user>")
      .action((x, c) => c.copy(adminUser = x))
      .text("Administrator user name whose to call zabbix http api (default is 'Admin')")

    opt[String]('p', "pass").optional()
      .valueName("<password>")
      .action((x, c) => c.copy(adminUser = x))
      .text("A password of the administrator user (default is 'zabbix')")

    opt[Unit]('j', "json").optional()
      .action((_, c) => c.copy(isJson = true))
      .text("Format outputs in JSON")

    opt[Unit]('d', "debug").optional()
      .action((_, c) => c.copy(isDebug = true))
      .text("Enable debug logging")
  }
}
