package com.github.zabbicook.util

import com.github.zabbicook.test.UnitSpec

import scala.util.Random

class TopologicalSortSpec extends UnitSpec {

  def ordered[T](array: Seq[T])(ahead: T, behind: T): Boolean = {
    array.indexOf(ahead) < array.indexOf(behind)
  }

  case class Element(name: String, dependencies: Seq[String])
  implicit val sortable: TopologicalSortable[Element] = TopologicalSortable[Element] { (node, _all) =>
    node.dependencies.map(d => _all.find(_.name == d)).flatten
  }
  "apply" should "sort T" in {
    val all = Seq(
      Element("e1", Seq("e2", "e3")),
      Element("e2", Seq("e4")),
      Element("e3", Seq("e2", "e4","e5")),
      Element("e4", Seq("e5")),
      Element("e5", Seq()),
      Element("e6", Seq("e1", "e4", "e5")),
      // another dag
      Element("e7", Seq("e8")),
      Element("e8", Seq()),
      // isolated
      Element("e9", Seq())
    )
    val Right(sorted) = TopologicalSort(Random.shuffle(all))
    val checkOrder = ordered[String](sorted.map(_.name).toSeq) _
    all.map { entity =>
      entity.dependencies.foreach { depends =>
        assert(checkOrder(entity.name, depends))
      }
    }
  }

  "applying to 2 elements" should "sort T" in {
    val all = Seq(
      Element("e1", Seq()),
      Element("e2", Seq("e1", "hoge"))
    )
    val Right(sorted) = TopologicalSort(Random.shuffle(all))
    assert(sorted === Seq(all(1), all(0)))
  }

  "apply" should "throws cyclic exception when a circular tree passed" in {
    val all = Seq(
      Element("e1", Seq("e2", "e3")),
      Element("e2", Seq("e4")),
      Element("e3", Seq("e2", "e4","e5")),
      Element("e4", Seq("e3"))
    )
    val Left(err) = TopologicalSort(Random.shuffle(all))
    assert(err.entities.exists(_.name == "e2"))
  }

  "apply" should "throws cyclic exception when a circular tree passed2" in {
    val all = Seq(
      Element("e1", Seq("e2")),
      Element("e2", Seq("e1")),
      Element("e3", Seq()),
      Element("e4", Seq())
    )
    val Left(err) = TopologicalSort(Random.shuffle(all))
    assert(err.entities.exists(_.name == "e2"))
  }
}
