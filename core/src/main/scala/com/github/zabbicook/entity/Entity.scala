package com.github.zabbicook.entity

abstract class Entity {
  def entityName: String = getClass.getName

  protected[this] def shouldBeUpdated[T](src: Option[T], dest: Option[T]): Boolean = {
    (src, dest) match {
      case (Some(s), Some(d)) => s != d
      case _ => false
    }
  }
}
