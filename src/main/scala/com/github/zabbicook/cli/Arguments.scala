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
  newPassword: Option[String] = None,
  showDoc: Boolean = false,
  docDepth: Int = Int.MaxValue,
  docRoot: String = ""
)

object Arguments {
  val parser = new scopt.OptionParser[Arguments]("zabbicook") {
    opt[File]('f', "file").optional()
      .valueName("<file>")
      .action((x, c) => c.copy(input = Some(x)))
      .text("(required) Path of Hocon format Zabbicook configuration file.")

    opt[String]('i', "uri").optional()
      .valueName("<uri>")
      .action((x, c) => c.copy(url = x))
      .text("Endpoint URI of zabbix API server. (default is 'http://localhost/')")

    opt[String]('u', "user").optional()
      .valueName("<user>")
      .action((x, c) => c.copy(user = x))
      .text("Zabbix administrator user's name (default is 'Admin').")

    opt[String]('p', "pass").optional()
      .valueName("<password>")
      .action((x, c) => c.copy(pass = x))
      .text(s"Zabbix administrator user's password (default is 'zabbix'). " +
        "The '--change-pass' option uses as the username of the user who changes the password.")

    opt[Unit]('j', "json").optional()
      .action((_, c) => c.copy(isJson = true))
      .text("Output in JSON format.")

    opt[Unit]("change-pass").optional()
      .action((_, c) => c.copy(setPassword = true))
      .text(s"Change the login password. '--user', '--pass', '--new-pass' options are also required. " +
        "Since this command is idempotent so if 'pass' word is already set, it does nothing.")

    opt[String]("new-pass").optional()
      .valueName("<new-password>")
      .action((x, c) => c.copy(newPassword = Some(x)))
      .text("New password. (required if '--change-pass' is specified.")

    opt[Unit]("doc").optional()
      .action((_, c) => c.copy(showDoc = true))
      .text("Show the configuration file schema in a tree. " +
        "Other options that can be used together. " +
        "'-L': specify the depth of the tree. '-r': The root of the tree.")

    opt[Int]('L', "level").optional()
      .valueName("<depth>")
      .action((x, c) => c.copy(docDepth = x))
      .text("The depth of the tree displayed by '--doc'.")

    opt[String]('r', "root").optional()
      .valueName("<root>")
      .action((x, c) => c.copy(docRoot = x))
      .text("The root of the tree displayed by '--doc'.")

    opt[Unit]('d', "debug").optional()
      .action((_, c) => c.copy(isDebug = true))
      .text("Enables debug logging.")

    opt[Unit]('v', "version").optional()
      .action((_, c) => c.copy(showVersion = true))
      .text("Shows version.")
  }
}
