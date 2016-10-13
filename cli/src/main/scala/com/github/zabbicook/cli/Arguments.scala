package com.github.zabbicook.cli

import java.io.File

case class Arguments(
  input: Option[File] = None,
  url: String = "http://localhost:8080/",
  adminUser: String = "Admin",
  adminPass: String = "zabbix",
  isJson: Boolean = false,
  isDebug: Boolean = false,
  showVersion: Boolean = false
)

object Arguments {
  val parser = new scopt.OptionParser[Arguments]("zabbicook") {
    opt[File]('f', "file").optional()
      .valueName("<file>")
      .action((x, c) => c.copy(input = Some(x)))
      .text("(required) Zabbicook configuration file in Hocon format.")

    opt[String]('i', "uri").optional()
      .valueName("<uri>")
      .action((x, c) => c.copy(url = x))
      .text("Endpoint URI of zabbix API server. (default is 'http://localhost/')")

    opt[String]('u', "user").optional()
      .valueName("<user>")
      .action((x, c) => c.copy(adminUser = x))
      .text("User name of zabbix administrator user. (default is 'Admin')")

    opt[String]('p', "pass").optional()
      .valueName("<password>")
      .action((x, c) => c.copy(adminUser = x))
      .text("Password of the administrator user. (default is 'zabbix')")

    opt[Unit]('j', "json").optional()
      .action((_, c) => c.copy(isJson = true))
      .text("To outpu in JSON format.")

    opt[Unit]('d', "debug").optional()
      .action((_, c) => c.copy(isDebug = true))
      .text("Enables debug logging.")

    opt[Unit]('v', "version").optional()
      .action((_, c) => c.copy(showVersion = true))
      .text("Shows version information.")
  }
}
