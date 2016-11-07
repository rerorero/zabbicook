val playVersion = "2.5.7"

parallelExecution in ThisBuild := false // due to use a single Zabbix stub server

val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-Ywarn-unused-import",
    "-Ywarn-unused"
  ),
  libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test"
  ),
  test in assembly := {}
)

val core = project.in(file("./core"))
  .settings(commonSettings: _*)
  .settings(
    name := "zabbicook-core",
    libraryDependencies ++= Seq(
      "com.ning" % "async-http-client" % "1.9.33",
      "com.typesafe.play" % "play-json_2.11" % playVersion,
      "ai.x" %% "play-json-extensions" % "0.8.0", // https://github.com/playframework/playframework/issues/3174
      "com.typesafe" % "config" % "1.3.0",
      "com.chuusai" %% "shapeless" % "2.3.2"
    )
  )

val cli = project.in(file("./cli"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "zabbicook-cli",
    resolvers += Resolver.sonatypeRepo("public"),
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.5.0"
    ),
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "com.github.zabbicook.cli",
    mainClass in assembly := Some("com.github.zabbicook.cli.Main"),
    assemblyJarName in assembly := { s"${name.value}-${version.value}.jar" }
  )
  .dependsOn(
    core,
    core % "test->test"
  )

val root = Project("zabbicook", file("./"))
  .aggregate(core, cli)

