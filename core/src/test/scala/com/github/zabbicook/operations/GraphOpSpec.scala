package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.Stored
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.graph._
import com.github.zabbicook.test.{TestGraphs, UnitSpec}

class GraphOpSpec extends UnitSpec with TestGraphs {
  val sut = graphOp

  def clean() = {
    cleanTestItems()
  }

  def checkPresented(expectedGraphs: Seq[GraphSetting], actuals: Seq[Graph[Stored]]): Unit = {
    assert(expectedGraphs.length === actuals.length)
    expectedGraphs.foreach { expected =>
      val actual = actuals.find(_.name == expected.graph.name).get
      assert(actual.shouldBeUpdated(expected.graph) === false)
      val actualItems = await(sut.getGraphItems(actual.getStoredId))
      assert(expected.items.length === actualItems.length)
      (expected.items.sortBy(_.color) zip actualItems.sortBy(_.color)) foreach {
        case (exp, act) =>
          assert(act.shouldBeUpdated(exp.toGraphItem(StoredId("dummy"))) === false)
      }
    }
  }

  "present" should "create, delete and update graph and graph items in templates" in {
    val graphAppend = testTemplates(0) -> Seq(
        GraphSetting(
          Graph(
            name = specName("graph X"),
            height = 400,
            width = 800,
            graphtype = Some(GraphType.pie)
          ),
          Seq(
            GraphItemSetting(
              color = "ABABAB",
              itemName = item0.name,
              drawtype = Some(DrawType.gradient)
            ),
            GraphItemSetting(
              color = "F6F6F6",
              itemName = item1.name
            )
          )
        ),
      GraphSetting(
        Graph(
          name = specName("graph Y"),
          height = 500,
          width = 900
        ),
        Seq(
          GraphItemSetting(
            color = "000000",
            itemName = item1.name
          )
        )
      )
    )

    cleanRun(clean) {

      // create a graph
      {
        presentTestGraphs()
        testGraphs.foreach { gs =>
          val Some(template) = await(templateOp.findByHostname(gs._1.template.host))
          val founds = await(sut.getBelongingGraphs(template.template.getStoredId))
          checkPresented(gs._2, founds)
        }
      }

      // not appended yet
      val _appendAt = graphAppend._1
      val appendAt = await(templateOp.findByHostnamesAbsolutely(Seq(_appendAt.template.host)).map(_.head))
      assert(Seq() === await(
        sut.getBelongingGraphs(appendAt.template.getStoredId).map(
          _.filter(stored => graphAppend._2.exists(gs => gs.graph.name == stored.name))
        )))

      // appends graph
      {
        val updates = testGraphs(_appendAt) ++ graphAppend._2
        val report = await(sut.present(appendAt.template.host, updates))
        assert(graphAppend._2.length === report.count)
        assert(graphAppend._2.head.graph.entityName === report.created.head.entityName)
        val founds = await(sut.getBelongingGraphs(appendAt.template.getStoredId))
        checkPresented(updates, founds)
        // represent does nothing
        val report2 = await(sut.present(appendAt.template.host, updates))
        assert(report2.isEmpty)
      }

      // update
      {
        val modifiedGraph = {
          val org = graphAppend._2.head
          val m = org.copy(graph = org.graph.copy(width = 555, height = 333, graphtype = Some(GraphType.stacked), yaxismax = Some(123.4)))
          graphAppend.copy(_2 = graphAppend._2.updated(0, m))
        }
        val modified = testGraphs(_appendAt) ++ modifiedGraph._2
        val report = await(sut.present(appendAt.template.host, modified))
        assert(2 === report.count) // deleted and created
        assert(graphAppend._2.head.graph.entityName === report.deleted.head.entityName)
        assert(graphAppend._2.head.graph.entityName === report.created.head.entityName)
        val founds = await(sut.getBelongingGraphs(appendAt.template.getStoredId))
        checkPresented(modified, founds)
      }

      // absent
      {
        val report = await(sut.present(_appendAt.template.host, testGraphs(_appendAt)))
        assert(graphAppend._2.length === report.count)
        assert(graphAppend._2.head.graph.entityName === report.deleted.head.entityName)
        val founds = await(sut.getBelongingGraphs(appendAt.template.getStoredId))
        checkPresented(testGraphs(_appendAt), founds)
        // reabsent does nothing
        val report2 = await(sut.present(_appendAt.template.host, testGraphs(_appendAt)))
        assert(report2.isEmpty())
      }
    }
  }

  "present" should "create, delete and update items in the graph setting" in {
    val itemAppend =
      GraphItemSetting(
        color = "DEAD00",
        itemName = item1.name,
        sortorder = Some(1),
        yaxisside = Some(YAxisSide.right)
      )

    cleanRun(clean) {
      presentTestGraphs()

      val key = testTemplates(0)
      val template = await(templateOp.findByHostnamesAbsolutely(Seq(key.template.host)).map(_.head))
      val orgGraphs = testGraphs(key)

      val appended = orgGraphs.updated(0, graph1.copy(items = graph1.items :+ itemAppend))

      // append a graph item
      {
        val report = await(sut.present(template.template.host, appended))
        assert(2 === report.count) // delete and create
        val founds = await(sut.getBelongingGraphs(template.template.getStoredId))
        checkPresented(appended, founds)
        // represent does nothing
        val report2 = await(sut.present(template.template.host, appended))
        assert(report2.isEmpty())
      }

      // update
      {
        val modItem = itemAppend.copy(color = "00AA00")
        val updated = appended.updated(0, graph1.copy(items = graph1.items.updated(0, modItem)))
        val report = await(sut.present(template.template.host, updated))
        assert(2 === report.count) // delete and create
        val founds = await(sut.getBelongingGraphs(template.template.getStoredId))
        checkPresented(updated, founds)
        // represent does nothing
        val report2 = await(sut.present(template.template.host, updated))
        assert(report2.isEmpty())
      }

      // absent
      {
        val removed = appended.updated(0, graph1.copy(items = graph1.items.tail))
        val report = await(sut.present(template.template.host, removed))
        assert(2 === report.count) // delete and create
        val founds = await(sut.getBelongingGraphs(template.template.getStoredId))
        checkPresented(removed, founds)
        // represent does nothing
        val report2 = await(sut.present(template.template.host, removed))
        assert(report2.isEmpty())
      }
    }
  }
}
