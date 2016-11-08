package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.graph.{Graph, GraphItem, GraphSetting}
import com.github.zabbicook.entity.item.Item
import com.github.zabbicook.entity.template.Template
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GraphOp(api: ZabbixApi, templateOp: TemplateOp, itemOp: ItemOp) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  /**
    * get graphs inherited from parent templates
    */
  def getInheritedGraphs(hostId: StoredId): Future[Seq[Graph[Stored]]] = findByTemplateId(hostId, inherited = true)

  /**
    * get graphs without inherited items
    */
  def getBelongingGraphs(hostId: StoredId): Future[Seq[Graph[Stored]]] = findByTemplateId(hostId, inherited = false)

  private[this] def findByTemplateId(templateId: StoredId, inherited: Boolean): Future[Seq[Graph[Stored]]] = {
    val params = Json.obj()
      .prop("templateids" -> templateId)
      .prop("output" -> "extend")
      .prop("inherited" -> inherited)
    api.requestAs[Seq[Graph[Stored]]]("graph.get", params)
  }

  def getGraphItems(graphId: StoredId): Future[Seq[GraphItem[Stored]]] = {
    val params = Json.obj()
      .prop("graphids" -> graphId)
      .prop("output" -> "extend")
    api.requestAs[Seq[GraphItem[Stored]]]("graphitem.get", params)
  }

  def create(templateId: StoredId, graph: Graph[NotStored], items: Seq[GraphItem[NotStored]]): Future[Report] = {
    require(items.forall(_.itemid.isInstanceOf[StoredId]))
    val params = Json.toJson(graph).as[JsObject]
        .prop("gitems" -> Json.toJson(items))
    api.requestSingleId("graph.create", params, "graphids")
      .map(id => Report.created(graph.toStored(id)))
  }

  def delete(graphs: Seq[Graph[Stored]]): Future[Report] = {
    deleteEntities(api, graphs, "graph.delete", "graphids")
  }


  // We don't present update method. Instead of update, delete and recreate are used.
  // Because according to the document, to delete graph items is not supported.
  // https://www.zabbix.com/documentation/3.2/manual/api/reference/graph/update

  private[this] def presentGraph(
    template: Template[Stored],
    itemsOfTemplate: Seq[Item[Stored]],
    graphSetting: GraphSetting,
    storedOpt: Option[Graph[Stored]]
  ): Future[Report] = {

    def itemSettingToItem(
      setting: GraphSetting
    ): Future[Seq[GraphItem[NotStored]]] = Future {
      setting.items.map { is =>
        val item = itemsOfTemplate.find(_.name == is.itemName).headOption.getOrElse(
          throw NoSuchEntityException(s"Template '${template.host}' has no such item '${is.itemName}'.")
        )
        is.toGraphItem(item.getStoredId)
      }
    }

    def itemsShoudBeUpdated(stored: Seq[GraphItem[Stored]], setting: Seq[GraphItem[NotStored]]): Boolean = {
      if (stored.length == setting.length) {
        val noDiff = (stored.sortBy(_.itemid) zip setting.sortBy(_.itemid)) forall {
          case (i1, i2) => !i1.shouldBeUpdated(i2)
        }
        !noDiff
      } else {
        true
      }
    }

    val gitemsFut = itemSettingToItem(graphSetting)

    storedOpt match {
      case None =>
        gitemsFut.flatMap { gitems =>
          create(template.getStoredId, graphSetting.graph, gitems)
        }

      case Some(stored) =>
        for {
          gitems <- gitemsFut
          storedItems <- getGraphItems(stored.getStoredId)
          report <- ((stored.shouldBeUpdated(graphSetting.graph)) || itemsShoudBeUpdated(storedItems, gitems)) match {
            case false =>
              Future(Report.empty())
            case true =>
              // recreates instead of update
              for {
                d <- delete(Seq(stored))
                c <- create(template.getStoredId, graphSetting.graph, gitems)
              } yield d + c
          }
        } yield {
          report
        }
    }
  }

  /**
    * Keep the status of the Graph (and the graph items) to which belongs the template to be constant.
    * If the graph specified by name does not exist, create it.
    * If already exists, it fills the gap.
    * @param templateHost Name of the template
    * @param graphSettings Setting of the graphs belonging to the template
    */
  def present(templateHost: String, graphSettings: Seq[GraphSetting]): Future[Report] = {
    for {
      _ <- showStartInfo(logger, graphSettings.length, s"graphs (and graph items) of template '${templateHost}'")
      template <- templateOp.findByHostnamesAbsolutely(Seq(templateHost)).map(_.head.template)
      templateItems <- itemOp.findByHostId(template.getStoredId)
      currentGraphs <- getBelongingGraphs(template.getStoredId)
      // Warn? (currentExisted, toBeDeleted) = currentGraphs.partition(cur => graphSettings.exists(_.graph.name == cur.name))
      x = currentGraphs.partition(cur => graphSettings.exists(_.graph.name == cur.name))
      currentExisted = x._1
      toBeDeleted = x._2
      deleted <- delete(toBeDeleted)
      createdAndUpdated <- traverseOperations(graphSettings) { g =>
        val currentOpt = currentExisted.find(_.name == g.graph.name)
        presentGraph(template, templateItems, g, currentOpt)
      }
    } yield {
      deleted + createdAndUpdated
    }
  }

  def absent(templateHost: String, graphSettings: Seq[GraphSetting]): Future[Report] = {
    for {
      currentGraphs <- templateOp.findByHostname(templateHost) flatMap {
        case Some(template) => getBelongingGraphs(template.template.getStoredId)
        case None => Future(Seq())
      }
      toBeDeleted = currentGraphs.filter(cur => graphSettings.exists(_.graph.name == cur.name))
      report <- delete(toBeDeleted)
    } yield report
  }
}
