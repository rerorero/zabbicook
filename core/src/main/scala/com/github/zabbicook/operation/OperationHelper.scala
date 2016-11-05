package com.github.zabbicook.operation

import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.Entity
import com.github.zabbicook.entity.Entity.Stored
import com.typesafe.scalalogging.Logger
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Extended Json object for zabbix api
  */
class OperationJsObj(val underlying: JsObject) {

  // appends the filter fields
  def filter[A](p: (String, A))(implicit w: Writes[A]): JsObject = {
    underlying ++ Json.obj("filter" -> Json.obj(p._1 -> Json.toJson(p._2)))
  }

  // appends the output field
  def outExtend(): JsObject = {
    underlying ++ Json.obj("output" -> "extend")
  }

  // appends a field
  def prop[A](param: (String, A))(implicit w: Writes[A]): JsObject = {
    underlying ++ Json.obj(param._1 -> param._2)
  }

  // appends if Some() value
  def propOpt[A](param: (String, Option[A]))(implicit w: Writes[A]): JsObject = {
    param._2 match {
      case Some(p) => underlying ++ Json.obj(param._1 -> p)
      case None => underlying
    }
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

class OptionFutureExtends[A](val underlying: Option[A]) {
  def flatFutureMap[B](f: A => Future[Option[B]]): Future[Option[B]] = {
    underlying match {
      case Some(a) => f(a)
      case None => Future.successful(None)
    }
  }

  def futureMap[B](f: A => Future[B]): Future[Option[B]] = {
    underlying match {
      case Some(a) => f(a).map(Some(_))
      case None => Future.successful(None)
    }
  }
}

trait OperationHelper {
  implicit def operationJsObj(o: JsObject): OperationJsObj = new OperationJsObj(o)

  implicit def optionFutureExtends[A](o: Option[A]): OptionFutureExtends[A] = new OptionFutureExtends[A](o)

  protected[this] def traverseOperations[A,B](in: Traversable[A])(f: A => Future[Report]): Future[Report] = {
    Future.traverse(in)(f).map(Report.flatten)
  }

  /**
    * call the delete api then returns deleted id and a report
    */
  protected[this] def deleteEntities[A <: Entity[Stored]](
    api: ZabbixApi,
    items: Seq[A],
    method: String,
    respondId: String
  ): Future[Report] = {
    if (items.isEmpty) {
      Future.successful(Report.empty())
    } else {
      val param = Json.toJson(items.map(_.getStoredId))
      api.requestIds(method, param, respondId)
        .map(_ => Report.deleted(items))
    }
  }

  protected[this] def showStartInfo(logger: Logger, count: Int, name: String): Future[Unit] = {
    Future {
      if (count > 0) logger.info(s"presenting $count $name ...")
    }
  }
}
