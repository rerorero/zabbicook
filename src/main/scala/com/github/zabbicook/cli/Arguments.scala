package com.github.zabbicook.cli

import java.io.File

case class Arguments(
  input: Option[File] = None,
  url: String = "http://localhost:8080/",
  user: String = "Admin",
  pass: String = "zabbix",
  isJson: Boolean = false,
  isDebug: Boolean = false,
  showVersion: Boolean = false,
  setPassword: Boolean = false,
  newPassword: Option[String] = None
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
      .action((x, c) => c.copy(user = x))
      .text("User name of zabbix administrator user. (default is 'Admin')")

    opt[String]('p', "pass").optional()
      .valueName("<password>")
      .action((x, c) => c.copy(pass = x))
      .text(s"""Password of the administrator user. (default is 'zabbix')
               |When '--change-pass' option specified, it is used as a old password.""".stripMargin)

    opt[Unit]('j', "json").optional()
      .action((_, c) => c.copy(isJson = true))
      .text("To outpu in JSON format.")

    opt[Unit]("change-pass").optional()
      .action((_, c) => c.copy(setPassword = true))
      .text(s"""Changes login password. Also required '--user', '--pass' and '--new-pass' options.
               |This command is idempotent so when already set the 'pass'word, it will do nothing and success.
               |ex. ${programName} --change-pass --user Admin --pass zabbix --new-pass NEWPASSWORD""".stripMargin)

    opt[String]("new-pass").optional()
      .valueName("<new-password>")
      .action((x, c) => c.copy(newPassword = Some(x)))
      .text("An new password. (required when '--change-pass' is specified.")

    opt[Unit]('d', "debug").optional()
      .action((_, c) => c.copy(isDebug = true))
      .text("Enables debug logging.")

    opt[Unit]('v', "version").optional()
      .action((_, c) => c.copy(showVersion = true))
      .text("Shows version information.")
  }
}
