package com.github.zabbicook.test

import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.NonFatal


abstract class UnitSpec extends FlatSpec with ScalaFutures {
  implicit val ex = ExecutionContext.global

  protected[this] val prefix = "_zabbicook_"
  def specName(s: String) = prefix + s

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(3, Seconds), interval = Span(300, Millis))


  def cleanRun(clean: => Unit)(f: => Unit): Unit = {
    try {
      clean
      f
    } finally clean
  }

  def await[T](f: => Future[T], withNullPointerException: Boolean = false): T = {
    val fut =
      try {
        f
      } catch {
        case NonFatal(e) =>
          e.printStackTrace()
          throw new Exception("An exception is thrown from an expression which returns a Future", e)
      }
    try {
      // it times out in 10 seconds on Travis CI
      Await.result(fut, 30 second)
    } catch {
      case e: NullPointerException if !withNullPointerException =>
        e.printStackTrace()
        throw new Exception("Maybe mock is not enough ...", e)
    }
  }
}
