package com.github.zabbicook.entity.host

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.prop.EntityCompanionMetaHelper
import com.github.zabbicook.entity.prop.Meta._

case class HostConf(
  host: Host[NotStored],
  hostGroups: Seq[String],
  interfaces: Seq[HostInterface[NotStored]],
  templates: Option[Seq[String]]
)

object HostConf extends EntityCompanionMetaHelper {
  override val meta = entity("Host configurations")(
    Host.required("host"),
    array("hostGroups")("groups","hostGroups")("(required) Host groups to add the host to."),
    arrayOf("interfaces")(HostInterface.required("interfaces")),
    arrayOf("templates")(HostInterface.optional("templates"))
  ) _
}
