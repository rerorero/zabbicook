package com.github.zabbicook.test

import com.github.zabbicook.entity.graph._
import com.github.zabbicook.entity.template.TemplateSettings
import com.github.zabbicook.operation._

trait TestGraphs extends TestItems { self: UnitSpec =>

  val graph1 =
    GraphSetting(
      Graph(
        name = specName("graph1"),
        height = 100,
        width = 400,
        graphtype = Some(GraphType.stacked),
        percent_left = Some(50),
        show_legend = Some(false),
        yaxismax = Some(200),
        ymax_type = Some(GraphMinMaxType.fixed)
      ),
      Seq(
        GraphItemSetting(
          color = "FF5555",
          itemName = item0.name,
          drawtype = Some(DrawType.dot),
          sortorder = Some(2),
          `type` = Some(GraphItemType.sum),
          yaxisside = Some(YAxisSide.right)
        ),
        GraphItemSetting(
          color = "012345",
          itemName = item1.name
        )
      )
    )

  val graph2 =
    GraphSetting(
      Graph(
        name = specName("graph2"),
        height = 200,
        width = 300,
        percent_right = Some(50),
        yaxismin = Some(10)
      ),
      Seq(
        GraphItemSetting(
          color = "0F0F0F",
          itemName = item0.name
        )
      )
    )

  val graph3 =
    GraphSetting(
      Graph(
        name = specName("graph3"),
        height = 300,
        width = 500
      ),
      Seq(
        GraphItemSetting(
          color = "0F0F0F",
          itemName = item1.name
        )
      )
    )
  protected[this] val testGraphs: Map[TemplateSettings.NotStoredAll, Seq[GraphSetting]] = Map(
    testTemplates(0) -> Seq(graph1, graph2),
    testTemplates(1) -> Seq(graph3)
  )

  def presentTestGraphs(ops: Ops): Unit = {
    presentTestItems(ops)
    testGraphs.foreach { case (t, graphs) =>
      await(ops.graph.present(t.template.host, graphs))
    }
  }

  def cleanTestGraphs(ops: Ops): Unit = {
    testGraphs.foreach { case (t, graphs) =>
      await(ops.graph.absent(t.template.host, graphs))
    }
    cleanTestItems(ops)
  }
}
