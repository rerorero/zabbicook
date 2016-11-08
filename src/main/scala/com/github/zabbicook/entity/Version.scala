package com.github.zabbicook.entity

case class Version (
  major1: Int,
  major2: Int,
  minor: Int
) extends Ordered[Version] {
  override def compare(that: Version): Int = {
    Version.ordering.compare(this, that)
  }
}

object Version {

  val ordering: Ordering[Version] = Ordering.by(unapply)

  def of(s: String): Version = {
    val ss = s.split("\\.")
    if (ss.length != 3) throw EntityException(s"Invalid version format: $s")
    try {
      Version(ss(0).toInt, ss(1).toInt, ss(2).toInt)
    } catch {
      case e: Throwable =>
        throw EntityException(s"Invalid version format: $s")
    }
  }

  def majorOf(n: Int) = Version(n, 0, 0)
}
