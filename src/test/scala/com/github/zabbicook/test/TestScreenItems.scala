package com.github.zabbicook.test

import com.github.zabbicook.entity.screen._
import com.github.zabbicook.operation.Ops

trait TestScreenItems extends TestScreens with TestGraphs { self: UnitSpec =>
  val graphResource = ScreenItemResource(Some(testTemplates(0).hostName), testGraphs(testTemplates(0))(0).graph.name)
  val itemResource = ScreenItemResource(Some(testTemplates(0).hostName), testItems(0).items(0).name)
  val hostgroupResource = ScreenItemResource(None, testHostGroups(0).name)
  val screenResource = ScreenItemResource(None, testScreens(2).name)
  //val hostResource = ScreenItemResource(None, testHosts(0).host.host)

  protected[this] val testScreenItemsFor0: Seq[ScreenItemSetting] = Seq(
    // history of action (resource is not required)
    ScreenItemSetting(
      resourcetype = ScreenResourceType.historyOfActions,
      halign = Some(HAlign.left),
      elements = Some(10),
      height = Some(100),
      width = Some(300),
      sort_triggers = Some(SortTrigger.statusAsc),
      valign = Some(VAlign.bottom),
      x = Some(2),
      y = Some(2)
    ),
    // graph
    ScreenItemSetting(
      resourcetype = ScreenResourceType.graph,
      resource = Some(graphResource),
      x = Some(2),
      y = Some(3)
    ),
    // simpleGraph
    ScreenItemSetting(
      resourcetype = ScreenResourceType.simpleGraph,
      resource = Some(itemResource),
      x = Some(2),
      y = Some(4)
    ),
    // trigger info
    ScreenItemSetting(
      resourcetype = ScreenResourceType.triggersInfo,
      resource = Some(hostgroupResource),
      x = Some(3),
      y = Some(3)
    ),
    // screen
    ScreenItemSetting(
      resourcetype = ScreenResourceType.screen,
      resource = Some(screenResource),
      x = Some(3),
      y = Some(4)
    )
//    // host issues
//    ScreenItemSetting(
//      resourcetype = ScreenResourceType.screen,
//      resource = Some(hostResource),
//      x = Some(4),
//      y = Some(5)
//    )
  )

  protected[this] val testScreenItemsFor1: Seq[ScreenItemSetting] = Seq(
    // history of events (resource is not required)
    ScreenItemSetting(
      resourcetype = ScreenResourceType.historyOfEvents,
      height = Some(400),
      width = Some(400),
      x = Some(1),
      y = Some(1)
    )
  )

  protected[this] val testScreenItems: Map[String, Seq[ScreenItemSetting]] = Map(
    testScreens(0).name -> testScreenItemsFor0,
    testScreens(1).name -> testScreenItemsFor1
  )

  def presentTestScreenItems(ops: Ops): Unit = {
    presentTestScreens(ops)
    presentTestGraphs(ops)
    testScreenItems.foreach(kv => await(ops.screenItem.present(kv._1, kv._2, None)))
  }

  def cleanTestScreenItems(ops: Ops): Unit = {
    cleanTestScreens(ops)
    cleanTestGraphs(ops)
  }
}
