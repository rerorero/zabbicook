package com.github.zabbicook.cli

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApiConf
import com.github.zabbicook.chef.Chef
import com.github.zabbicook.cli.RunResult.{FileNotFound, NoInputSpecified, OtherError, ParseError, RunSuccess}
import com.github.zabbicook.hocon.{HoconError, HoconReader, HoconSuccess}
import com.github.zabbicook.operation.Ops
import com.github.zabbicook.recipe.Recipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class Runner(conf: Arguments) extends Logging {

  import com.github.zabbicook.hocon.HoconReadsCompanion._

  import com.github.zabbicook.hocon.HoconReads._

  val apiConf = new ZabbixApiConf(
    apiPath = conf.url.toString,
    authUser = conf.user,
    authPass = conf.pass,
    interval = conf.apiInterval
  )

  def run(): Future[RunResult] = {
    val operationSet = Ops.create(apiConf)
    val chef = new Chef(operationSet)

    configureLogging()
    presentRecipe(chef)
      .andThen {
        case _ => operationSet.close()
      }
  }

  private[this] def configureLogging(): Unit = {
    if (conf.isDebug) {
      // set debug level anyway if debug enabled
      Logging.debugging()
    } else if (conf.isJson) {
      // When formatted in Json, disables logging to be able to parse output as Json when failure cases.
      Logging.silent()
    } else {
      Logging.info()
    }
  }

  private[this] def presentRecipe(chef: Chef): Future[RunResult] = {
    conf.input match {
      case Some(f) if (!f.exists() || !f.isFile()) => Future.successful(FileNotFound(f))
      case Some(f) =>
        (HoconReader.read[Recipe](f, Recipe.optional("root")) match {
          case HoconSuccess(recipe) =>
            // TODO validation all entities (avoid duplicated names, etc..)
            chef.present(recipe).map(RunSuccess)
          case e: HoconError =>
            Future.successful(ParseError(e))
        }).recover {
          case NonFatal(e) => OtherError(e)
        }
      case None => Future.successful(NoInputSpecified)
    }
  }

  def changePassword(alias: String, newPassword: String, oldPassword: String): Future[RunResult] = {
    val operationSet = Ops.create(apiConf)
    configureLogging()
    operationSet.user.changePassword(alias, newPassword, oldPassword)
      .map(RunSuccess)
      .recover { case NonFatal(e) => OtherError(e) }
      .andThen { case _ => operationSet.close() }
  }
}
