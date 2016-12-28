package com.github.zabbicook.test

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.item._
import com.github.zabbicook.entity.prop.EnabledEnum
import com.github.zabbicook.operation.{ItemOp, Ops, Report}

import scala.concurrent.Future

trait TestItems extends TestTemplates { self: UnitSpec =>

  protected[this] val testApp0 = "zabbicook test app 0"
  protected[this] val testApp1 = "zabbicook test app 1"
  protected[this] val testApp2 = "zabbicook test app 2"

  case class TestItemsSetting(
    template: String,
    applications: Seq[String],
    items: Seq[Item[NotStored]]
  )

  protected[this] val item0: Item[NotStored] = Item(
    delay = 300,
    `key_` = "vfs.file.cksum[/var/log/messages]",
    name = specName("item appended file"),
    `type` = ItemType.ZabbixAgent,
    value_type = ValueType.unsigned,
    units = Some("B"),
    history = Some(7),
    trends = Some(10),
    applicationNames = Some(Seq(testApp0, testApp1))
  )

  protected[this] val item1: Item[NotStored] = Item(
    delay = 60,
    `key_` = "sysUpTime",
    name = specName("item appended snmp"),
    `type` = ItemType.SNMPv2Agent,
    value_type = ValueType.unsigned,
    units = Some("B"),
    data_type = Some(DataType.decimal),
    formula = Some(0.01),
    multiplier = Some(EnabledEnum.`true`),
    snmp_community = Some("mycommunity"),
    snmp_oid = Some("SNMPv2-MIB::sysUpTime.0"),
    port = Some(8161),
    applicationNames = Some(Seq(testApp0))
  )

  protected[this] val item2: Item[NotStored] = Item(
    delay = 120,
    `key_` = """jmx["java.lang:type=Compilation",Name]""",
    name = specName("item appended name of JIT"),
    `type` = ItemType.JMXagent,
    value_type = ValueType.character
  )

  protected[this] val testItems: Seq[TestItemsSetting] = Seq(
    TestItemsSetting(
      testTemplates(0).template.host,
      Seq(testApp0, testApp1),
      Seq(item0, item1)
    ),
    TestItemsSetting(
      testTemplates(1).template.host,
      Seq(testApp2),
      Seq(item2)
    )
  )

  def presentItems(op: ItemOp, settings: Seq[TestItemsSetting]): Report = {
    await(Future.traverse(settings)(s => op.presentWithTemplate(s.template, s.applications, s.items))
      .map(Report.flatten))
  }

  def presentTestItems(ops: Ops): Unit = {
    presentTestTemplates(ops)
    presentItems(ops.item, testItems)
  }

  def cleanTestItems(ops: Ops): Unit = {
    await(ops.item.absentWithTemplate(
      testItems.map(i => (i.template, i.items.map(_.name))).toMap
    ))
    cleanTestTemplates(ops)
  }
}
