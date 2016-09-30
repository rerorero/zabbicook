package com.github.zabbicook.hocon

import com.github.zabbicook.entity.ValidationResult.{Invalid, Valid}
import com.github.zabbicook.entity._
import com.typesafe.config.{Config, ConfigFactory, ConfigValue}

import scala.collection.JavaConversions._

trait HoconReads[T] { self =>
  def read(o: Config): HoconResult[T]

  def acceptableKeys: Option[Set[String]] = None

  def map[U](f: T => U): HoconReads[U] = HoconReads[U] { conf => self.read(conf).map(f) }

  def flatMap[U](f: T => HoconReads[U]): HoconReads[U] = HoconReads[U] { conf =>
    self.read(conf).flatMap(f(_).read(conf))
  }

  def withAcceptableKeys(keys: String*): HoconReads[T] =
    HoconReads.withAcceptableKeys(keys.toSet)(self.read)
}

object HoconReads {
  def apply[T](f: Config => HoconResult[T]): HoconReads[T] = new HoconReads[T] {
    override def read(o: Config): HoconResult[T] = f(o)
  }

  def withAcceptableKeys[T](keys: Set[String])(f: Config => HoconResult[T]): HoconReads[T] = new HoconReads[T] {
    override def read(o: Config): HoconResult[T] = f(o)
    override val acceptableKeys: Option[Set[String]] = Some(keys)
  }

  def of[T](implicit reads: HoconReads[T]) = reads

  case class ConfigFunc[T] (
    get: (Config, String) => HoconResult[Option[T]]
  ) {
    // Is it right way ..?
    // Option.get used because get() will not fail due to the key not existing
    def fromValue(c: ConfigValue): HoconResult[T] = get(c.atKey(dummyKey), dummyKey).map(_.get)

    def map[U](f: T => U): ConfigFunc[U] = ConfigFunc((c,s) => this.get(c,s).map(_.map(f)))
  }

  object ConfigFunc {
    def gen[T](f: (Config, String) => T): ConfigFunc[T] = {
      ConfigFunc( (config: Config, path: String) => {
        if (config.hasPath(path)) HoconResult(Some(f(config, path))) else HoconSuccess(None)
      })
    }
  }

  implicit val stringConfigFunc: ConfigFunc[String] = ConfigFunc.gen(_.getString(_))

  implicit val numberConfigFunc: ConfigFunc[Number] = ConfigFunc.gen(_.getNumber(_))

  implicit val intConfigFunc: ConfigFunc[Int] = ConfigFunc.gen(_.getInt(_))

  implicit val longConfigFunc: ConfigFunc[Long] = ConfigFunc.gen(_.getLong(_))

  implicit val doubleConfigFunc: ConfigFunc[Double] = ConfigFunc.gen(_.getDouble(_))

  implicit val booleanConfigFunc: ConfigFunc[Boolean] = ConfigFunc.gen(_.getBoolean(_))

  private[this] val dummyKey = "_dummy"

  implicit def arrayConfigFunc[T](implicit tf: ConfigFunc[T]): ConfigFunc[Seq[T]] = ConfigFunc((config, path) => {
    if (config.hasPath(path)) {
      HoconResult.sequence(config.getList(path))(c => tf.fromValue(c)).map(Some(_))
    } else {
      HoconSuccess(None)
    }
  })

  implicit def setConfigFunc[T](implicit cf: ConfigFunc[T]): ConfigFunc[Set[T]] =
    arrayConfigFunc(cf).map(_.toSet)

  implicit def objetConfigFunc[T](implicit reads: HoconReads[T]): ConfigFunc[T] = ConfigFunc((config, path) => {
    if (config.hasPath(path)) {
      val obj = config.getObject(path)
      // verify names of the fields in the object are all acceptable
      val keys = reads.acceptableKeys.map(valids => (valids, obj.keySet() -- valids))
      keys match {
        case Some((valids, invalids)) if !invalids.isEmpty =>
          HoconError.UnrecognizedKeys(config.origin(), invalids.toSet, valids, path)
        case _ =>
          reads.read(obj.toConfig).map(Some(_))
      }
    } else {
      HoconSuccess(None)
    }
  })

  implicit def mapConfigFunc[T](implicit tf: ConfigFunc[T]): ConfigFunc[Map[String, T]] = ConfigFunc((config, path) => {
    if (config.hasPath(path)) {
      val l = HoconResult.sequence(config.getObject(path).entrySet()) { entry =>
        // Is this right way...?
        tf.fromValue(entry.getValue).map(v => entry.getKey -> v)
      }
      l.map(l => Some(l.toMap))
    } else {
      HoconSuccess(None)
    }
  })

  private[this] def enumValidResult[T <: EnumProp](config: Config, path: String)(value: Option[T]): HoconResult[Option[T]] = {
    value match {
      case Some(v) =>
        v.validate() match {
          case Valid => HoconSuccess(Some(v))
          case e: Invalid => HoconError.InvalidConditionProperty(config.origin(), e, path)
        }
      case None =>
        HoconSuccess(None)
    }
  }

  implicit def stringEnumConfigFunc[T <: StringEnumProp](implicit f: String => T): ConfigFunc[T] = ConfigFunc((config, path) => {
    implicitly[ConfigFunc[String]].get(config, path).map(_.map(f)).flatMap(enumValidResult(config, path))
  })

  implicit def numEnumDescribedWithStringConfigFunc[T <: NumberEnumDescribedWithString](implicit f: String => T): ConfigFunc[T] = ConfigFunc((config, path) => {
    implicitly[ConfigFunc[String]].get(config, path).map(_.map(f)).flatMap(enumValidResult(config, path))
  })

  implicit def enabledEnumConfigFunc[T <: EnabledEnum](implicit f: Boolean => T): ConfigFunc[T] = ConfigFunc((config, path) => {
    implicitly[ConfigFunc[Boolean]].get(config, path).map(_.map(f)).flatMap(enumValidResult(config, path))
  })

  implicit def enabledEnumZeroPositiveConfigFunc[T <: EnabledEnumZeroPositive](implicit f: Boolean => T): ConfigFunc[T] = ConfigFunc((config, path) => {
    implicitly[ConfigFunc[Boolean]].get(config, path).map(_.map(f)).flatMap(enumValidResult(config, path))
  })

  def optional[T](p: String)(implicit func: ConfigFunc[T]): HoconReads[Option[T]] = HoconReads(func.get(_, p))

  def required[T](p: String)(implicit func: ConfigFunc[T]): HoconReads[T] = {
    HoconReads[T] { conf =>
      func.get(conf, p).flatMap {
        case Some(v) => HoconSuccess(v)
        case None => HoconError.NotExist(conf.origin(), p)
      }
    }
  }

  private[this] def configFuncForMapToSet[T](keyName: String)(implicit cf: ConfigFunc[T]): ConfigFunc[Set[T]] = {
    ConfigFunc((conf, path) => {
      if (conf.hasPath(path)) {
        val l = HoconResult.sequence(conf.getObject(path)) { case (k, v) =>
          val merge = ConfigFactory.parseMap(Map(keyName -> k))
          val c = v.withFallback(merge)
          cf.fromValue(c)
        }
        l.map(s => Some(s.toSet))
      } else {
        HoconSuccess(None)
      }
    })
  }

  /**
    * Generates HoconReads which parse Hocon Map to Set[T].
    * The HoconReads parses Hocon Map which has keys to which T belong (as required property)
    * Both examples shown below are parsed same as Set[User] ('alias' is required and 'name' is optional property)
    * {{{
    *   """{ users: [ {alias:"Joey",name:"Ramone"}, {alias:"C.J",name:"Ramone"} ] }""" // Hocon
    *   optional[Set[User]] // HoconReads
    * }}}
    * {{{
    *   """{ users: {"Joey": {name:"Ramone"}, "C.J": {name:"Ramone"} }""" // Hocon
    *   optionalMapToSet[User] // HoconReads
    * }}}
    *
    * When the value at the requested path is missing, returns None
    * If the value is requisite, use requiredMapToSet().
    * @param path requested path
    * @param keyName property name of T, which used as keys of Hocon map
    * @param tf
    * @tparam T
    * @return
    */
  def optionalMapToSet[T](path: String, keyName: String)(implicit tf: ConfigFunc[T]): HoconReads[Option[Set[T]]] = {
    optional(path)(configFuncForMapToSet(keyName))
  }

  def requiredMapToSet[T](path: String, keyName: String)(implicit tf: ConfigFunc[T]): HoconReads[Set[T]] = {
    required(path)(configFuncForMapToSet(keyName))
  }
}

