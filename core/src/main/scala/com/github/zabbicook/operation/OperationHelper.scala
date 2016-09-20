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
}

trait OperationHelper {
  implicit def operationJsObj(o: JsObject): OperationJsObj = new OperationJsObj(o)

  protected[this] def traverseOperations[A,B](in: Seq[A])(f: A => Future[(B, Report)]): Future[(Seq[B], Report)] = {
    Future.traverse(in)(f).map { results =>
      results.foldLeft((Seq.empty[B], Report.empty)) {
        case (acc, (result, report)) => (result +: acc._1, report + acc._2)
      }
    }
  }
}
