package com.github.zabbicook.operation

import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Zabbix extended Json object
  */
class OperationJsObj(val underlying: JsObject) {

  def filter[A](p: (String, A))(implicit w: Writes[A]): JsObject = {
    underlying ++ Json.obj("filter" -> Json.obj(p._1 -> Json.toJson(p._2)))
  }

  def outExtend(): JsObject = {
    underlying ++ Json.obj("output" -> "extend")
  }

  def prop[A](param: (String, A))(implicit w: Writes[A]): JsObject = {
    underlying ++ Json.obj(param._1 -> param._2)
  }

  def propIf[A](cond: => Boolean)(param: (String, A))(implicit w: Writes[A]): JsObject = {
    if (cond) prop(param) else underlying
  }

  def propIfDefined[A,B](cond: => Option[B])(f: B => (String, A))(implicit w: Writes[A]): JsObject = {
    cond match {
      case Some(b) => prop(f(b))
      case None => underlying
    }
  }
}

trait OperationHelper {
  implicit def operationJsObj(o: JsObject): OperationJsObj = new OperationJsObj(o)

  protected[this] def traverseOperations[A,B](in: Traversable[A])(f: A => Future[(B, Report)]): Future[(Seq[B], Report)] = {
    Future.traverse(in)(f).map(foldReports)
  }

  protected[this] def foldReports[A](reports: Traversable[(A, Report)]): (Seq[A], Report) = {
    reports.foldLeft((Seq.empty[A], Report.empty)) {
      case (acc, (result, report)) => (result +: acc._1, report + acc._2)
    }
  }
}
