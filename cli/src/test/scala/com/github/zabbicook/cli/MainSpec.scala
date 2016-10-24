package com.github.zabbicook.cli

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.graph._
import com.github.zabbicook.entity.host._
import com.github.zabbicook.entity.item.{DataType, Item, ItemType, ValueType}
import com.github.zabbicook.entity.media.{Media, MediaType, MediaTypeType}
import com.github.zabbicook.entity.prop.EnabledEnum
import com.github.zabbicook.entity.template.{Template, TemplateSettings}
import com.github.zabbicook.entity.trigger.Severity
import com.github.zabbicook.entity.user._
import com.github.zabbicook.operation.{Ops, StoredHost}
import com.github.zabbicook.test.{TestConfig, UnitSpec}

import scala.concurrent.Future

class MainSpec extends UnitSpec with TestConfig {

  def runMain(
    host: String,
    filePath: Option[String],
    debug: Boolean = false
  ): (Int, List[String]) = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[String]
    def mockedPrinter() = new Printer {
      override def out(msg: String): Unit = buf.append(msg)
      override def error(msg: String): Unit = buf.append(msg)
    }

    val i = Seq("-i", host)
    val f = filePath.map(s => Seq("-f", s)).getOrElse(Seq())
    val d = if (debug) Seq("-d") else Seq()
    val printer = mockedPrinter()
    val code = await(new Main(printer).run(
      (i ++ f ++ d).toArray
    ))

    (code, buf.toList)
  }

  "CLI Main" should "parse and configure all from files" in {
    val op = new Ops(cachedApi)

    // expected
    val mediaTypes = Seq(
      MediaType[NotStored](
        description = "zabbicook-spec-media1",
        `type` = MediaTypeType.email,
        smtp_email = Some("test@example.com"),
        smtp_helo = Some("zabbicook.example.com"),
        smtp_server = Some("example.com"),
        status = true
      ),
      MediaType[NotStored](
        description = "zabbicook-spec-media2",
        `type` = MediaTypeType.script,
        exec_path = Some("test.sh"),
        exec_params = Some("""{ALERT.SENDTO}
                             |{ALERT.SUBJECT}
                             |{ALERT.MESSAGE}
                             |""".stripMargin),
        status = false
      )
    )

    val hostGroups = Seq(
      HostGroup(name = "zabbicook-spec hostgroup1"),
      HostGroup(name = "zabbicook-spec hostgroup2"),
      HostGroup(name = "zabbicook-spec hostgroup3")
    )

    val userGroups: Seq[UserGroup[NotStored]] = Seq(
      UserGroup(name = "zabbicook-spec usergroup1", debug_mode = Some(false), users_status = Some(true)),
      UserGroup(name = "zabbicook-spec usergroup2")
    )

    val users: Seq[(User[NotStored], Seq[UserGroup[NotStored]], Seq[Media[NotStored]])] = Seq(
      (User(alias = "Zabbicook-spec-Alice", autologin = Some(true), lang = Some("en"),
        theme = Some(Theme.dark), `type` = Some(UserType.superAdmin)),
        Seq(userGroups(0)),
        Seq(
          Media(active = true, period="1-7,00:00-24:00", sendto = "dest", severity = MediaConfig.severitiesToBinary(Seq(Severity.information, Severity.warning))),
          Media(active = false, period="1-2,10:00-20:00", sendto = "anyone", severity = MediaConfig.severitiesToBinary(Seq()))
        )
        ),
      (User(alias = "Zabbicook-spec-Bob"), Seq(userGroups(1)), Seq())
    )

    val templates: Seq[TemplateSettings.NotStoredAll] = Seq(
      TemplateSettings(
        Template(host = "zabbicook-spec template 1", name = Some("template1"), description = Some("hello")),
        Seq(HostGroup(name = "zabbicook-spec hostgroup1")),
        Some(Seq(Template(host = "zabbicook-spec template 2"), Template(host = "Template OS Linux")))
      ),
      TemplateSettings(
        Template(host = "zabbicook-spec template 2"),
        Seq(HostGroup(name = "zabbicook-spec hostgroup1"), HostGroup(name = "zabbicook-spec hostgroup2")),
        None
      )
    )

    val items: Map[String, Seq[Item[NotStored]]] = Map(
      "zabbicook-spec template 1" -> Seq(Item(
        delay = 120,
        `key_` = """jmx["java.lang:type=Compilation",Name]""",
        name = "zabbicook-spec item0",
        `type` = ItemType.JMXagent,
        value_type = ValueType.character
      )),
      "zabbicook-spec template 2" -> Seq(Item(
        delay = 300,
        `key_` = "vfs.file.cksum[/var/log/messages]",
        name = "zabbicook-spec item1",
        `type` = ItemType.ZabbixAgent,
        value_type = ValueType.unsigned,
        units = Some("B"),
        history = Some(7),
        trends = Some(10)
      ), Item(
        delay = 60,
        `key_` = "sysUpTime",
        name = "zabbicook-spec item2",
        `type` = ItemType.SNMPv2Agent,
        value_type = ValueType.unsigned,
        data_type = Some(DataType.decimal),
        formula = Some(0.01),
        multiplier = Some(EnabledEnum.enabled),
        snmp_community = Some("mycommunity"),
        snmp_oid = Some("SNMPv2-MIB::sysUpTime.0"),
        port = Some(8161)
      ))
    )

    val graphMaps: Map[String, Seq[GraphSetting]] = Map(
      "zabbicook-spec template 1" -> Seq(
        GraphSetting(
          Graph(
            name = "zabbicook-spec graph1",
            height = 200,
            width = 300
          ),
          Seq(
            GraphItemSetting(
              color = "123456",
              itemName = "Host local time",
              drawtype = Some(DrawType.bold)
            )
          )
        )
      ),
      "zabbicook-spec template 2" -> Seq(
        GraphSetting(
          Graph(
            name = "zabbicook-spec graph2",
            height = 200,
            width = 600,
            graphtype = Some(GraphType.pie),
            percent_left = Some(50),
            show_legend = Some(false),
            yaxismax = Some(200),
            ymax_type = Some(GraphMinMaxType.fixed)
          ),
          Seq(
            GraphItemSetting(
              color = "252525",
              itemName = "zabbicook-spec item1",
              drawtype = Some(DrawType.dot),
              sortorder = Some(2),
              `type` = Some(GraphItemType.sum),
              yaxisside = Some(YAxisSide.right)
            ),
            GraphItemSetting(
              color = "012345",
              itemName = "zabbicook-spec item2"
            )
          )
        ),
        GraphSetting(
          Graph(
            name = "zabbicook-spec graph3",
            height = 300,
            width = 500
          ),
          Seq(
            GraphItemSetting(
              color = "000000",
              itemName = "zabbicook-spec item1"
            )
          )
        )
      )
    )

    val hosts = Seq(
      HostConf(
        Host(
          host="zabbicook-spec host1",
          description = Some("host1"),
          inventory_mode = Some(InventoryMode.automatic),
          ipmi_authtype = Some(IpmiAuthAlgo.MD5),
          ipmi_password = Some("pass"),
          ipmi_privilege = Some(IpmiPrivilege.operator),
          ipmi_username = Some("user"),
          name = Some("visible host name 1"),
          status = Some(false)
        ),
        hostGroups = Seq("zabbicook-spec hostgroup1"),
        interfaces = Seq(
          HostInterface(
            dns = Some("zabbicook.spec.host1.com"),
            main = true,
            port = "10001",
            `type` = InterfaceType.agent,
            useip = InterfaceUseIp.dns
          ),
          HostInterface(
            ip = Some("127.0.0.1"),
            main = false,
            port = "10002",
            `type` = InterfaceType.agent,
            useip = InterfaceUseIp.ip
          ),
          HostInterface(
            ip = Some("127.0.0.1"),
            main = true,
            port = "10003",
            `type` = InterfaceType.SNMP,
            useip = InterfaceUseIp.ip,
            bulk = Some(false)
          )
        ),
        templates = Some(Seq("zabbicook-spec template 2"))
      ),
      HostConf(
        Host(host="zabbicook-spec host2",status = Some(true)),
        Seq("zabbicook-spec hostgroup2","zabbicook-spec hostgroup3"),
        Seq(
          HostInterface(
            ip = Some("127.0.0.2"),
            main = true,
            port = "10001",
            `type` = InterfaceType.agent,
            useip = InterfaceUseIp.ip
          )
        ),
        None
      )
    )

    def clean(): Unit = {
      await(op.host.absent(hosts.map(_.host.host)))
      await(op.item.absentWithTemplate(items.mapValues(_.map(_.name))))
      await(op.template.absent(templates.map(_.template.host)))
      await(op.user.absent(users.map(_._1.alias)))
      await(op.userGroup.absent(userGroups.map(_.name)))
      await(op.hostGroup.absent(hostGroups.map(_.name)))
      await(Future.traverse(graphMaps) { case (template, graphs) => op.graph.absent(template, graphs)})
      await(op.mediaType.absent(mediaTypes.map(_.description)))
    }

    def check(): Unit = {
      // media types
      val actualMediaTypes = await(op.mediaType.findByDescriptions(mediaTypes.map(_.description)))
      assert(mediaTypes.length === actualMediaTypes.length)
      actualMediaTypes.foreach { actual =>
        val Some(expected) = mediaTypes.find(_.description == actual.description)
        assert(false === actual.shouldBeUpdated(expected))
      }

      // host groups
      val actualHostGroups = await(op.hostGroup.findByNames(hostGroups.map(_.name)))
      assert(hostGroups.length === actualHostGroups.length)
      (actualHostGroups.sortBy((_.name)) zip hostGroups.sortBy((_.name))) foreach { case (actual, expected) =>
        assert(expected.name === actual.name)
      }
      // user groups
      val actualUserGroups = await(op.userGroup.findByNames(userGroups.map(_.name)))
      assert(userGroups.length === actualUserGroups.length)
      (actualUserGroups.sortBy((_._1.name)) zip userGroups.sortBy((_.name))) foreach { case (actual, expected) =>
        assert(false === actual._1.shouldBeUpdated(expected))
      }
      // users
      val actualUsers = await(op.user.findByAliases(users.map(_._1.alias)))
      assert(users.length === actualUsers.length)
      (actualUsers.sortBy(_._1.alias) zip users.sortBy(_._1.alias)) foreach { case (actual, expected) =>
        assert(false === actual._1.shouldBeUpdated(expected._1))
        assert(expected._2.map(_.name).toSet === actual._2.map(_.name).toSet)
        val medias = await(op.user.findUserMedias(actual._1.getStoredId))
        assert(expected._3.length === medias.length)
      }
      // templates
      val actualTemplates = await(op.template.findByHostnames(templates.map(_.template.host)))
      assert(templates.length === actualTemplates.length)
      (actualTemplates.sortBy(_.template.host) zip templates.sortBy(_.template.host)) foreach { case (actual, expected) =>
        assert(false === actual.template.shouldBeUpdated(expected.template))
        assert(expected.groupsNames === actual.groupsNames)
        assert(expected.linkedTemplateHostNames === actual.linkedTemplateHostNames)
      }
      // items
      items.foreach { case (template, itemSeq) =>
        val templateId = actualTemplates.map(_.template).find(_.host == template).get.getStoredId
        val actuals = await(op.item.getBelongingItems(templateId))
        assert(itemSeq.length === actuals.length)
        (itemSeq.sortBy(_.`key_`) zip actuals.sortBy(_.`key_`)) foreach { case (expected, actual) =>
          assert(false === actual.shouldBeUpdated(expected))
          assert(expected.name === actual.name)
          assert(expected.delay === actual.delay)
          assert(expected.`type` === actual.`type`)
        }
      }
      // graphs
      graphMaps.foreach { case (template, graphSettings) =>
        val templateId = actualTemplates.map(_.template).find(_.host == template).get.getStoredId
        val actuals = await(op.graph.getBelongingGraphs(templateId))
        assert(graphSettings.length === actuals.length)
        (graphSettings.sortBy(_.graph.name) zip actuals.sortBy(_.name)) foreach { case (expected, actual) =>
            assert(actual.shouldBeUpdated(expected.graph) === false)
            val actualItems = await(op.graph.getGraphItems(actual.getStoredId))
            assert(expected.items.length === actualItems.length)
            (expected.items.sortBy(_.color) zip actualItems.sortBy(_.color)) foreach { case (expItem, actItem) =>
              assert(actItem.shouldBeUpdated(expItem.toGraphItem(StoredId("dummy"))) === false)
            }
        }
      }

      // hosts
      hosts.foreach { case HostConf(host, groupNames, interfaces, templatNames) =>
        //val templateIds = op.template.findByHostnamesAbsolutely(templatNames.getOrElse(Seq()))
        val Some(StoredHost(storedHost,storedIfs,storedGroups,storedTemplates)) =
          await(op.host.findByHostname(host.host))
        assert(false === storedHost.shouldBeUpdated(host))
        assert(groupNames.toSet === storedGroups.map(_.name).toSet)
        assert(interfaces.length === storedIfs.length)
        assert(storedIfs.forall(s => interfaces.exists(_.isIdentical(s))))
        assert(templatNames.getOrElse(Seq()).toSet === storedTemplates.toSet)
      }
    }

    cleanRun(clean) {
      val path = getClass.getResource("/mainspec/zabbicook.conf").getPath()
      val (code, out) = runMain("http://localhost:8080/", Some(path))
      if (code != 0) println(out.foreach(println))
      assert(0 === code)
      check()

      // rerun does not affect
      val (code2, output2) = runMain("http://localhost:8080/", Some(path))
      assert(0 === code2)
      assert(1 === output2.length)
      check()
    }
  }
}
