package com.github.zabbicook.util

import scala.concurrent.{ExecutionContext, Future}

object Futures {
  def sequential[T, U](in: Traversable[T])(f: T => Future[U])(implicit ec: ExecutionContext): Future[Seq[U]] = {
    in.foldLeft(Future(Seq.empty[U])) { (acc, item) =>
      for {
        results <- acc
        next <- f(item)
      } yield results :+ next
    }
  }
}
