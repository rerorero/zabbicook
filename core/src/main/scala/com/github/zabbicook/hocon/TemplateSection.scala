package com.github.zabbicook.hocon

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.item.Item
import com.github.zabbicook.operation.TemplateSettings
import HoconReads._

case class TemplateSection(
  settings: TemplateSettings.NotStoredAll,
  items: Seq[Item[NotStored]]
)

object TemplateSection {
  implicit val hoconReads: HoconReads[TemplateSection] = {
    for {
      settings <- of[TemplateSettings.NotStoredAll]
      items <- optional[Seq[Item[NotStored]]]("items")
    } yield {
      TemplateSection(
        settings,
        items.getOrElse(Seq())
      )
    }
  }
}
