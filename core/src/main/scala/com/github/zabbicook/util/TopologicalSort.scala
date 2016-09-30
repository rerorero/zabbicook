package com.github.zabbicook.util

import scala.annotation.tailrec

trait TopologicalSortable[T] {
  def dependencies(node: T, allNodes: Traversable[T]): Iterable[T]
}

object TopologicalSortable {
  def apply[T](depends: (T, Traversable[T]) => Iterable[T]) = new TopologicalSortable[T] {
    override def dependencies(node: T, allNodes: Traversable[T]): Iterable[T] = depends(node, allNodes)
  }
}

object TopologicalSort {

  case class CyclicException[T](entities: Iterable[T]) extends Exception(s"Found cyclic references in topological sorting. ${entities}")

  private[this] case class Node[T](
    underlying: T
  ) {
    def dependencies(allNodes: Traversable[T])(implicit sortable: TopologicalSortable[T]): Iterable[Node[T]] =
      sortable.dependencies(underlying, allNodes).map(Node(_))
  }

  private[this] case class Edge[T](src: Node[T], dest: Node[T])

  private[this] case class Dag[T](nodes: Set[Edge[T]])

  /**
    * topological sort
    * @param values all nodes
    * @param sortable provides dependencies of a node
    * @tparam T type of node
    * @return Success: sorted values.
    *         Failure[CyclieException]: found cyclic references in values.
    */
  def apply[T](values: Traversable[T])(implicit sortable: TopologicalSortable[T]): Either[CyclicException[T], Iterable[T]] = {
    val nodes = values.map(Node(_))
    val edges = nodes.map(n => n.dependencies(values).map(Edge(n, _))).flatten
    val allDependencies = nodes.map(_.dependencies(values)).flatten
    val isolations = nodes.filter(t => !allDependencies.exists(_ == t)).filter(_.dependencies(values).isEmpty)
    val edgeMaps = edges.foldLeft(Map.empty[Node[T], Set[Node[T]]]) { (acc, e) =>
      acc + (e.src -> acc.getOrElse(e.src, Set())) + (e.dest -> (acc.getOrElse(e.dest, Set()) + e.src))
    }

    @tailrec
    def tsort(partMaps: Map[Node[T], Set[Node[T]]], done: Iterable[Node[T]]): Iterable[Node[T]] = {
      val (leaves, notLeaves) = partMaps.partition(_._2.isEmpty)
      if (leaves.isEmpty) {
        if (notLeaves.isEmpty)
          done
        else {
          throw CyclicException(notLeaves.keys.filter(k => !isolations.exists(_ == k)).take(3).map(_.underlying))
        }
      } else {
        val found = leaves.map(_._1)
        tsort(notLeaves.mapValues(_ -- found), done ++ found)
      }
    }

    try {
      val ordered = tsort(edgeMaps, Seq())
      Right((ordered ++ isolations).map(_.underlying))
    } catch {
      case e: CyclicException[_] => Left(e.asInstanceOf[CyclicException[T]])
    }
  }

}
