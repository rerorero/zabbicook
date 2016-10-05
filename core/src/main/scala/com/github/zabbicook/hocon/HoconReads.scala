package com.github.zabbicook.hocon

import com.github.zabbicook.entity.ValidationResult.{Invalid, Valid}
import com.github.zabbicook.entity.prop._
import com.typesafe.config._

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

  case class ConfigFunc[T](
    atKey: (Config, String) => HoconResult[Option[T]],
    fromValue: (ConfigValue) => HoconResult[Option[T]]
  ) {
    def map[U](f: T => U): ConfigFunc[U] =
      ConfigFunc(
        (c, s) => this.atKey(c, s).map(_.map(f)),
        (c) => this.fromValue(c).map(_.map(f))
      )

    def flatMapWithOrigin[U](f: (T, ConfigOrigin) => HoconResult[Option[U]]) =
      ConfigFunc(
        (c, s) => this.atKey(c, s).flatMap {
          case Some(t) => f(t, c.origin())
          case None => HoconSuccess(None)
        },
        (c) => this.fromValue(c).flatMap {
          case Some(t) => f(t, c.origin())
          case None => HoconSuccess(None)
        }
      )
  }

  private[this] def expectTypedAs[A](value: ConfigValue, expectType: ConfigValueType)(expect: => HoconResult[Option[A]]): HoconResult[Option[A]] = {
    if (value.valueType() == ConfigValueType.NULL)
      HoconSuccess(None)
    else if (value.valueType() == expectType)
      expect
    else
      new HoconError(s"Expected type ${expectType} but the actual type is ${value.valueType()}", Some(value.origin()))
  }

  object ConfigFunc {
    def gen[T](atKey: (Config, String) => T, valueType: ConfigValueType): ConfigFunc[T] = {
      ConfigFunc(
        (c, path) => {
          if (c.hasPath(path)) HoconResult(Some(atKey(c, path))) else HoconSuccess(None)
        },
        (c) => expectTypedAs(c, valueType)(HoconSuccess(Some(c.unwrapped().asInstanceOf[T])))
      )
    }
  }

  implicit val stringConfigFunc: ConfigFunc[String] = ConfigFunc.gen(_.getString(_), ConfigValueType.STRING)

  implicit val numberConfigFunc: ConfigFunc[Number] = ConfigFunc.gen(_.getNumber(_), ConfigValueType.NUMBER)

  implicit val intConfigFunc: ConfigFunc[Int] = ConfigFunc.gen(_.getInt(_), ConfigValueType.NUMBER)

  implicit val longConfigFunc: ConfigFunc[Long] = ConfigFunc.gen(_.getLong(_), ConfigValueType.NUMBER)

  implicit val doubleConfigFunc: ConfigFunc[Double] = ConfigFunc.gen(_.getDouble(_), ConfigValueType.NUMBER)

  implicit val booleanConfigFunc: ConfigFunc[Boolean] = ConfigFunc.gen(_.getBoolean(_), ConfigValueType.BOOLEAN)

  private[this] def arrayToHoconResult[T](list: ConfigList)(implicit cf: ConfigFunc[T]): HoconResult[Option[Seq[T]]] = {
    HoconResult.sequence(list)(cf.fromValue).map(_.flatten).map(Some(_))
  }

  implicit def arrayConfigFunc[T](implicit cf: ConfigFunc[T]): ConfigFunc[Seq[T]] = ConfigFunc(
    (config, path) => {
      if (config.hasPath(path)) {
        arrayToHoconResult(config.getList(path))(cf)
      } else {
        HoconSuccess(None)
      }
    },
    (c) => expectTypedAs(c, ConfigValueType.LIST)(
      arrayToHoconResult(c.asInstanceOf[ConfigList])(cf)
    )
  )

  implicit def setConfigFunc[T](implicit cf: ConfigFunc[T]): ConfigFunc[Set[T]] =
    arrayConfigFunc(cf).map(_.toSet)

  private[this] def objToHoconResult[T](obj: ConfigObject)(implicit reads: HoconReads[T]): HoconResult[Option[T]] = {
    // verify names of the fields in the object are all acceptable
    val keys = reads.acceptableKeys.map(valids => (valids, obj.keySet() -- valids))
    keys match {
      case Some((valids, invalids)) if !invalids.isEmpty =>
        HoconError.UnrecognizedKeys(obj.origin(), invalids.toSet, valids)
      case _ =>
        reads.read(obj.toConfig).map(Some(_))
    }
  }

  implicit def objetConfigFunc[T](implicit reads: HoconReads[T]): ConfigFunc[T] = ConfigFunc(
    (config, path) => {
      if (config.hasPath(path)) {
        objToHoconResult(config.getObject(path))
      } else {
        HoconSuccess(None)
      }
    },
    (c) => expectTypedAs(c, ConfigValueType.OBJECT)(
      objToHoconResult(c.asInstanceOf[ConfigObject])
    )
  )

  private[this] def objToMap[T](obj: ConfigObject)(cf: ConfigFunc[T]): HoconResult[Option[Map[String, T]]] = {
    obj.entrySet().foldLeft(HoconResult(Map.empty[String, T])) {
      case (HoconSuccess(acc), entry) =>
        cf.fromValue(entry.getValue) match {
          case HoconSuccess(None) => HoconSuccess(acc)
          case HoconSuccess(Some(value)) => HoconSuccess(acc + (entry.getKey -> value))
          case e: HoconError => e
        }
      case (err, _) => err
    }.map(Some(_))
  }

  implicit def mapConfigFunc[T](implicit tf: ConfigFunc[T]): ConfigFunc[Map[String, T]] = ConfigFunc(
    (config, path) => {
      if (config.hasPath(path)) {
        objToMap(config.getObject(path))(tf)
      } else {
        HoconSuccess(None)
      }
    },
    (c) => expectTypedAs(c, ConfigValueType.OBJECT)(
      objToMap(c.asInstanceOf[ConfigObject])(tf)
    )
  )

  private[this] def enumValidResult[T <: EnumProp](value: T, origin: ConfigOrigin): HoconResult[Option[T]] = {
    value.validate() match {
      case Valid => HoconSuccess(Some(value))
      case e: Invalid => HoconError.InvalidConditionProperty(origin, e)
    }
  }

  implicit def stringEnumConfigFunc[T <: StringEnumProp](implicit f: String => T): ConfigFunc[T] = {
    implicitly[ConfigFunc[String]].flatMapWithOrigin((s, origin) => enumValidResult(f(s), origin))
  }

  implicit def numEnumDescribedWithStringConfigFunc[T <: IntEnumDescribedWithString](implicit f: String => T): ConfigFunc[T] = {
    implicitly[ConfigFunc[String]].flatMapWithOrigin((s, origin) => enumValidResult(f(s), origin))
  }

  implicit def enabledEnumConfigFunc[T <: EnabledEnum](implicit f: Boolean => T): ConfigFunc[T] = {
    implicitly[ConfigFunc[Boolean]].flatMapWithOrigin((s, origin) => enumValidResult(f(s), origin))
  }

  implicit def enabledEnumZeroPositiveConfigFunc[T <: EnabledEnumZeroPositive](implicit f: Boolean => T): ConfigFunc[T] = {
    implicitly[ConfigFunc[Boolean]].flatMapWithOrigin((s, origin) => enumValidResult(f(s), origin))
  }

  def optional[T](p: String)(implicit func: ConfigFunc[T]): HoconReads[Option[T]] = HoconReads(func.atKey(_, p))

  def required[T](p: String)(implicit func: ConfigFunc[T]): HoconReads[T] = {
    HoconReads[T] { conf =>
      func.atKey(conf, p).flatMap {
        case Some(v) => HoconSuccess(v)
        case None => HoconError.NotExist(conf.origin(), p)
      }
    }
  }
}

