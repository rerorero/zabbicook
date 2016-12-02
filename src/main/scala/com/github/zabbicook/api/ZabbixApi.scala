package com.github.zabbicook.api

import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

import com.github.zabbicook.Logging
import com.github.zabbicook.entity.EntityId.StoredId
import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, Response}
import play.api.libs.json._

import scala.concurrent.{Future, Promise}
import scala.util.Random

class ZabbixApi(conf: ZabbixApiConf) extends Logging {
  implicit val executionContext = conf.executionContext

  private[this] val logger = defaultLogger

  private[this] val client = new AsyncHttpClient(conf.httpClientConfig)

  private[this] val authToken = new AtomicReference[String]("")

  private[this] val sequence = new Random

  private[this] val throttle: Throttle = {
    if (conf.concurrency && conf.interval.isZero) {
      NonStrictThrottle
    } else {
      new StrictThrottle(
        concurrency = if (conf.concurrency) 10 else 1,
        timeout = conf.timeout.multipliedBy(3),
        startInterval = conf.interval
      )
    }
  }

  /**
    * close resource handles
    */
  def close(): Unit = client.close()

  /**
    * @see https://www.zabbix.com/documentation/3.0/manual/api
    * @param method method property value
    * @param param params property value
    * @param auth if method requires authorization, set true.
    * @return result property in Json which api responded
    */
  def request(method: String, param: JsValue, auth: Boolean = true): Future[JsValue] = {
    for {
      tokenJs <- if (auth) login() else Future.successful(JsNull)
      ret <- requestWithAuth(method, param, tokenJs)
    } yield ret
  }

  /**
    * expects only a result or empty (in case method responds result as array)
    * Throws NotSingleExeption if zabbix api responds two or more results
    */
  def requestSingle(method: String, param: JsValue, auth: Boolean = true): Future[Option[JsValue]] = {
    requestAs[JsArray](method, param, auth) map {
      case JsArray(Seq()) => None
      case JsArray(Seq(single)) => Some(single)
      case JsArray(els) => throw NotSingleException(method, els.length)
    }
  }

  def requestAs[T](method: String, param: JsValue, auth: Boolean = true)(implicit reads: Reads[T]): Future[T] = {
    request(method, param, auth).map(jsonTo(method, _))
  }

  def requestSingleAs[T](method: String, param: JsValue, auth: Boolean = true)(implicit reads: Reads[T]): Future[Option[T]] = {
    requestSingle(method, param, auth).map(_.map(result => jsonTo(method, result)))
  }

  /**
    * for apis (such as xxx.create method) which returns object which has only an array of IDs
    * @param idsKey key name of an array property of IDs
    */
  def requestIds(method: String, param: JsValue, idsKey: String, auth: Boolean = true)(implicit reads: Reads[StoredId]): Future[Seq[StoredId]] = {
    requestAs[JsObject](method, param) map { js =>
      js.value.get(idsKey) match {
        case Some(keys) =>
          jsonTo[Seq[StoredId]](method, keys)
        case None =>
          logger.error(s"Failed to parse ${method} respond result: ${js}")
          sys.error(s"${method} response does not match ${reads.getClass.getName}")
      }
    }
  }

  def requestSingleId(method: String, param: JsValue, idsKey: String, auth: Boolean = true)(implicit reads: Reads[StoredId]): Future[StoredId] = {
    requestIds(method, param, idsKey, auth).map { seq =>
      // TODO in case of fail with non empty seq, it should be clean.
      if (seq.length != 1) throw NotSingleException(method, seq.length)
      seq.head
    }
  }

  private[this] def jsonTo[T](method: String, js: JsValue)(implicit reads: Reads[T]): T = {
    Json.fromJson[T](js) match {
      case JsSuccess(t, _) => t
      case JsError(err) =>
        logger.error(s"Failed to parse ${method} respond result: ${Json.prettyPrint(js)}")
        logger.error(s"parsing errors: ${err}")
        sys.error(s"${method} response does not match ${reads.getClass.getName}")
    }
  }

  private[this] def login(force: Boolean = false): Future[JsValue] = {
    if (!authToken.get().isEmpty) {
      val js = JsString(authToken.get())
      Future.successful(js)
    } else {
      // Although not thread-safe strictly, in most cases it will not be a problem...
      val params = Json.obj(
        "user" -> JsString(conf.authUser),
        "password" -> JsString(conf.authPass)
      )
      request("user.login", params, auth = false).map {
        case js@JsString(token) =>
          authToken.set(token)
          js
        case els =>
          logger.error(s"user.login respond: ${els}")
          sys.error("user.login respond unexpected format.")
      }
    }
  }

  private val DBEXECUTE_ERR_RETRY_MAX = 3
  private val TIMEOUT_RETRY_MAX = 2
  def requestWithAuth(method: String, param: JsValue, auth: JsValue, retries: Int = 0): Future[JsValue] = {
    val id = sequence.nextInt()
    val paramJson = Json.obj(
      "jsonrpc" -> JsString(conf.jsonrpc),
      "method" -> JsString(method),
      "params" -> param,
      "id" -> JsNumber(id),
      "auth" -> auth
    )

    val promise = Promise[Response]
    val handler = new AsyncCompletionHandler[Unit] {
      override def onCompleted(response: Response): Unit = {
        throttle.end()
        promise.success(response)
      }
      override def onThrowable(t: Throwable): Unit = {
        throttle.end()
        promise.failure(t)
      }
    }

    throttle.start()
    client.preparePost(conf.jsonRpcUrl)
      .setHeader("Content-Type", s"application/json-rpc")
      .setBody(paramJson.toString)
      .execute(handler)

    logger.debug(s">>> API POST ($id)")
    logger.debug(Json.prettyPrint(paramJson))

    def retry(): Future[JsValue] = {
      val sleep = retries * 100
      Thread.sleep(sleep)
      requestWithAuth(method, param, auth, retries + 1)
    }

    promise.future.flatMap { r =>
      logger.debug(s"<<< API RESPONSE ($id)")
      logger.debug(s"status = ${r.getStatusCode}")
      logger.debug(s"body = ${r.getResponseBody}")

      r.getStatusCode match {
        case code if (code / 100) == 2 =>
          Json.parse(r.getResponseBody()) match {
            case JsObject(m) if m.contains("result") =>
              Future.successful(m.get("result").get)
            case JsObject(m) if m.contains("error") =>
              Json.fromJson[ZabbixErrorResponse](m.get("error").get) match {
                case JsSuccess(err, _) if err.data.contains("DBEXECUTE_ERROR") && DBEXECUTE_ERR_RETRY_MAX > retries =>
                  // This is a workaround.
                  // Although I do not know the cause, DBEXECUTE_ERROR or timeout will occur
                  // in the first POST API call after the zabbix server is started for the first time.
                  // I know it will happen on docker and vagrant, but it may happen in other environments as well.
                  // To avoid this, retry will only be done at DBEXECUTE_ERROR.
                  logger.warn(s"${method} request failed(DBEXECUTE_ERROR). (${retries + 1} times)")
                  retry()
                case JsSuccess(err, _) =>
                  Future.failed(ErrorResponseException(method, err, s"(retried $retries times)"))
                case JsError(errors) =>
                  logger.error(s"Request parameter: ${Json.prettyPrint(paramJson)}")
                  logger.error(s"${method} respond ${r.getResponseBody()}")
                  logger.error(s"json errors: ${errors}")
                  sys.error(s"${method} responds unknown error")
              }
            case els =>
              logger.error(s"Request parameter: ${Json.prettyPrint(paramJson)}")
              logger.error(s"${method} respond: ${els}")
              sys.error(s"${method} responds unknown format")
          }
        case code =>
          logger.error(s"Request parameter: ${Json.prettyPrint(paramJson)}")
          logger.error(s"${method} respond ${r.getResponseBody()}")
          Future.failed(new ApiException(s"Unkown status($code) respond"))
      }
    } recoverWith {
      case e: TimeoutException if TIMEOUT_RETRY_MAX > retries =>
        // This is also workaround for issue for the first time api calling.
        logger.warn(s"${method} request failed(timeout). (${retries + 1} times)")
        retry()
    }
  }

  def getVersion(): Future[Version] = {
    request("apiinfo.version", JsArray(), auth = false).map {
      case JsString(value) =>
        Version.of(value)
      case els =>
        sys.error(s"apiinfo.version returns unexpected json.: ${els.toString}")
    }
  }
}
