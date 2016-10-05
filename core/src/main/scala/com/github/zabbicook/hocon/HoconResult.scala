package com.github.zabbicook.hocon

import com.github.zabbicook.entity.ValidationResult.Invalid
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
}

case class HoconSuccess[T](value: T) extends HoconResult[T]

class HoconError(detail: String, origin: Option[ConfigOrigin] = None, cause: Option[Exception] = None) extends HoconResult[Nothing] {
  override def toString: String = {
    origin.map(_.description()).getOrElse("Somewhere") + ": " + detail
  }
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
    message.replace(origin.description(), "")
  }

  case class NotExist(origin: ConfigOrigin, path: String)
    extends HoconError(s"does not have required property: '${path}'", Some(origin))

  case class TypeMismatched(e: ConfigException.WrongType)
    extends HoconError(removeOrigin(e.getMessage(), e.origin()), Some(e.origin()), Some(e))

  case class ParseFailed(e: ConfigException.Parse)
    extends HoconError(removeOrigin(e.getMessage(), e.origin()), Some(e.origin()), Some(e))

  case class InvalidConditionProperty(origin: ConfigOrigin, error: Invalid)
    extends HoconError(error.detail, Some(origin))

  case class UnrecognizedKeys(origin: ConfigOrigin, invalids: Set[String], acceptables: Set[String])
    extends HoconError(s"Unrecognized fields (${invalids.mkString(", ")}). Valid fields are (${acceptables.mkString(", ")})'", Some(origin))

  def from(e: ConfigException): HoconError = {
    e match {
      case ee: ConfigException.WrongType => TypeMismatched(ee)
      case ee: ConfigException.Parse => ParseFailed(ee)
      case _ => new HoconError(removeOrigin(e.getMessage, e.origin()), Some(e.origin()), Some(e))
    }
  }
}

