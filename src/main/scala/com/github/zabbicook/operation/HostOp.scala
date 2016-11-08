package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.StoredId
import com.github.zabbicook.entity.host.{Host, HostConf, HostGroup, HostInterface}
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class StoredHost(
  host: Host[Stored],
  interfaces: Seq[HostInterface[Stored]],
  hostGroups: Seq[HostGroup[Stored]],
  templateNames: Seq[String]
)

private[operation] case class TemplateName(name: String)

private[operation] object TemplateName {
  implicit val reads: Format[TemplateName] = Json.format[TemplateName]
}

class HostOp(api: ZabbixApi, hostGroupOp: HostGroupOp, templateOp: TemplateOp) extends OperationHelper with Logging {

  private[this] val logger = defaultLogger

  def findByHostname(hostname: String): Future[Option[StoredHost]] = {
    val params = Json.obj()
      .filter("host" -> Seq(hostname))
      .outExtend()
      .prop("selectGroups" -> "extend")
      .prop("selectInterfaces" -> "extend")
      .prop("selectParentTemplates" -> Seq("name"))
    api.requestSingleAs[JsObject]("host.get", params).map(_.map(maptToStoredHost))
  }

  def findByHostnames(hostnames: Seq[String]): Future[Seq[StoredHost]] = {
    Future.traverse(hostnames)(findByHostname).map(_.flatten)
  }

  private[this] def maptToStoredHost(root: JsValue): StoredHost = {
    val host = root.asOpt[Host[Stored]].getOrElse(sys.error(s"host.get returns unexpected formats.: ${Json.prettyPrint(root)}"))
    val interfaces = (root \ "interfaces").asOpt[Seq[HostInterface[Stored]]].getOrElse(sys.error(s"host.get returns no interfaces."))
    val groups = (root \ "groups").asOpt[Seq[HostGroup[Stored]]].getOrElse(sys.error(s"host.get returns no host groups"))
    val templates = (root \ "parentTemplates").asOpt[Seq[TemplateName]].getOrElse(sys.error(s"host.get returns no parentTemplates."))
    StoredHost(host, interfaces, groups, templates.map(_.name))
  }

  def create(
    host: Host[NotStored],
    groups: Seq[StoredId],
    interfaces: Seq[HostInterface[NotStored]],
    templateIds: Seq[StoredId]
  ): Future[Report] = {
    val groupIds = groups.map(g => Json.obj("groupid" -> g))
    val ifs = interfaces.map(i => Json.toJson(i.resolveDnsOrIp).as[JsObject])
    val templates = templateIds.map(id => Json.obj("templateid" -> id))
    val param = Json.toJson(host).as[JsObject]
      .prop("interfaces" -> ifs)
      .prop("groups" -> groupIds)
      .prop("templates" -> templates)
    api.requestSingleId("host.create", param, "hostids")
      .map(id => Report.created(host.toStored(id)))
  }

  def delete(hosts: Seq[Host[Stored]]): Future[Report] = {
    deleteEntities(api, hosts, "host.delete", "hostids")
  }

  private[this] def shouldInterfacesBeUpdated(stored: Seq[HostInterface[Stored]], conf: Seq[HostInterface[NotStored]]): Boolean = {
    if (stored.length == conf.length) {
      val zipped = stored.map(s => conf.find(_.isIdentical(s)).map((s,_))).flatten
      if (zipped.length == stored.length) {
        val noNeeds = zipped.forall(tpl => !tpl._1.shouldBeUpdated(tpl._2))
        !noNeeds
      } else {
        true
      }
    } else {
      true
    }
  }

  def present(hostConf: HostConf): Future[Report] = {
    for {
      storedOpt <- findByHostname(hostConf.host.host)
      groups <- hostGroupOp.findByNames(hostConf.hostGroups)
      _ <-  if (groups.isEmpty) {
              Future.failed(NoAvailableEntities(s"No available host groups for ${hostConf.host.host} (specified: ${hostConf.hostGroups})"))
            } else Future(Unit)
      templates <- templateOp.findByHostnamesAbsolutely(hostConf.templates.getOrElse(Seq()))
      r <- storedOpt match {
        case Some(stored) if shouldInterfacesBeUpdated(stored.interfaces, hostConf.interfaces) ||
                             stored.hostGroups.map(_.name).toSet != hostConf.hostGroups.toSet ||
                             stored.host.shouldBeUpdated(hostConf.host) ||
                             stored.templateNames.toSet != hostConf.templates.map(_.toSet).getOrElse(Set()) =>
          // Not to update but to delete and recreate because updating gracefully is bother.
          for {
            d <- delete(Seq(stored.host))
            c <- create(hostConf.host, groups.map(_.getStoredId), hostConf.interfaces, templates.map(_.template.getStoredId))
          } yield d + c
        case Some(_) =>
          Future(Report.empty())
        case None =>
          create(hostConf.host, groups.map(_.getStoredId), hostConf.interfaces, templates.map(_.template.getStoredId))
      }
    } yield {
      r
    }
  }

  def present(hostConfs: Seq[HostConf]): Future[Report] = {
    showStartInfo(logger, hostConfs.length, "hosts").flatMap(_ =>
      traverseOperations(hostConfs)(present)
    )
  }

  def absent(hostNames: Seq[String]): Future[Report] = {
    for {
      stored <- findByHostnames(hostNames)
      r <- delete(stored.map(_.host))
    } yield r
  }
}
