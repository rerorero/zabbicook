package com.github.zabbicook.hocon

import com.github.zabbicook.entity.prop.{EntityMeta, Meta}
import com.typesafe.config.{ConfigException, ConfigOrigin}

sealed trait HoconResult[+T] {
  def map[U](f: T => U): HoconResult[U] = this match {
    case HoconSuccess(v) => HoconSuccess(f(v))
    case e: HoconError => e
  }

  def flatMap[U](f: T => HoconResult[U]): HoconResult[U] = this match {
    case HoconSuccess(v) => f(v)
    case e: HoconError => e
  }

  def orElse[U >: T](f: => HoconResult[U]): HoconResult[U]
}

case class HoconSuccess[T](value: T) extends HoconResult[T] {
  def orElse[U >: T](f: => HoconResult[U]): HoconResult[U] = this
}

class HoconError(
  detail: String,
  origin: Option[ConfigOrigin] = None,
  meta: Option[Meta] = None,
  cause: Option[Throwable] = None
) extends HoconResult[Nothing] {

  override def toString: String = {
    origin.flatMap(Option(_)).map(_.description()).getOrElse("Somewhere") + ": " + detail +
      meta.map(m => System.lineSeparator() + m.description).getOrElse("")
  }

  def orElse[U >: Nothing](f: => HoconResult[U]): HoconResult[U] = f

  def throwableOpt: Option[Throwable] = cause
}

object HoconResult {
  def apply[T](v: => T): HoconResult[T] = {
    try {
      HoconSuccess(v)
    } catch {
      case e: ConfigException => HoconError.from(e)
    }
  }

  def sequence[A, B](seq: TraversableOnce[A])(f: A => HoconResult[B]): HoconResult[Seq[B]] = {
    seq.foldLeft(HoconResult(List.empty[B])) {
      case (HoconSuccess(acc), a) => f(a).map(_ +: acc)
      case (err, _) => err
    }.map(_.reverse)
  }
}

object HoconError {
  // remove origin descriptions from a message if contains redundantly
  private[this] def removeOrigin(message: String, origin: ConfigOrigin): String = {
    if (origin != null)
      message.replace(origin.description(), "")
    else
      message
  }

  case class NotExist(origin: ConfigOrigin, meta: Meta)
    extends HoconError(s"Required field ${meta.formatAliases} is not found", Some(origin), Some(meta))

  case class TypeMismatched(detail: String, origin: ConfigOrigin, meta: Option[Meta], cause: Option[Exception] = None)
    extends HoconError(detail, Some(origin), meta, cause)
  object TypeMismatched {
    def of(e: ConfigException.WrongType): TypeMismatched =
      TypeMismatched(removeOrigin(e.getMessage(), e.origin()), e.origin(), None, Some(e))
    def of(origin: ConfigOrigin, msg: String, meta: Meta): TypeMismatched =
      TypeMismatched(msg, origin, Some(meta))
  }

  case class ParseFailed(e: ConfigException.Parse)
    extends HoconError(removeOrigin(e.getMessage(), e.origin()), Some(e.origin()), None, Some(e))

  case class NotAcceptableValue(origin: ConfigOrigin, value: String, meta: Meta)
  extends HoconError(s"'$value' is not possible for ${meta.formatAliases}", Some(origin), Some(meta))

  case class UnrecognizedFields(origin: ConfigOrigin, invalids: Traversable[String], meta: EntityMeta)
    extends HoconError(s"Unrecognized fields (${invalids.mkString(", ")})", Some(origin), Some(meta))

  case class UnknownError(origin: ConfigOrigin, msg: String)
    extends HoconError(msg, Some(origin))

  def from(e: ConfigException): HoconError = {
    e match {
      case ee: ConfigException.WrongType => TypeMismatched.of(ee)
      case ee: ConfigException.Parse => ParseFailed(ee)
      case _ => new HoconError(removeOrigin(e.getMessage, e.origin()), Some(e.origin()), None, Some(e))
    }
  }
}

