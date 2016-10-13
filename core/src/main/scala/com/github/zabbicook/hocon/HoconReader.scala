package com.github.zabbicook.hocon

import java.io.File

import com.github.zabbicook.entity.EntityId.NotStoredId
import com.github.zabbicook.entity._
import com.github.zabbicook.entity.prop._
import com.typesafe.config._
import shapeless._

import scala.collection.JavaConversions._

object HoconReader {
  def read[T](v: => Config, meta: Meta)(implicit reader: HoconReads[T]): HoconResult[T] = {
    HoconReads.withConfigException2(reader.read(v, meta))
  }

  def read[T](file: File, meta: Meta)(implicit reader: HoconReads[T]): HoconResult[T] = {
    val opt = ConfigParseOptions.defaults().setAllowMissing(false)
    read(ConfigFactory.parseFile(file, opt), meta)
  }

  def read[T](s: String, meta: Meta)(implicit reader: HoconReads[T]): HoconResult[T] = {
    read(ConfigFactory.parseString(s), meta)
  }
}

trait HoconReads[T] { self =>
  def read(o: Config, meta: Meta): HoconResult[T]

  def fromValue(v: ConfigValue, meta: Meta): HoconResult[T] = {
    HoconError.UnknownError(v.origin(), s"fromValue function has not been implemented. ${v.valueType()} meta=${meta}")
  }

  def map[U](f: T => U): HoconReads[U] = HoconReads.of[U]((conf, meta) =>
    self.read(conf, meta).map(f)
  )

  def flatMap[U](f: T => HoconReads[U]): HoconReads[U] = HoconReads.of[U]((conf, meta) =>
    self.read(conf, meta).flatMap(f(_).read(conf, meta))
  )
}

object HoconReads {
  def apply[T](implicit reads: HoconReads[T]) = reads

  def of[T](f: (Config, Meta) => HoconResult[T]): HoconReads[T] = new HoconReads[T] {
    override def read(o: Config, m: Meta): HoconResult[T] = f(o, m)
  }

  def withConfigException[T](f: => T): HoconResult[T] = {
    withConfigException2(HoconSuccess(f))
  }

  def withConfigException2[T](f: => HoconResult[T]): HoconResult[T] = {
    try {
      f
    } catch {
      case e: ConfigException => HoconError.from(e)
    }
  }

  def getByMetas[T](conf: Config, meta: Meta, get: (Config, Meta, String) => HoconResult[T]): HoconResult[T] = {
    meta.aliases.find(conf.hasPath(_)) match {
      case Some(alias) =>
        get(conf, meta, alias)
      case None => HoconError.NotExist(conf.origin(), meta)
    }
  }

  private[this] def configFuncOf[T](expectType: ConfigValueType, f: (Config, String) => T): HoconReads[T] = {
    new HoconReads[T] {
      override def read(conf: Config, meta: Meta): HoconResult[T] = {
        getByMetas(conf, meta, (c, _, key) => withConfigException(f(c, key)))
      }

      override def fromValue(v: ConfigValue, meta: Meta): HoconResult[T] = {
        if (v.valueType() == expectType)
          HoconSuccess(v.unwrapped().asInstanceOf[T])
        else if (v.valueType() == ConfigValueType.NULL)
          HoconError.NotAcceptableValue(v.origin(), "null", meta)
        else
          new HoconError(s"Expected type ${expectType} but the actual type is ${v.valueType()}", Some(v.origin()))
      }
    }
  }

  implicit val string: HoconReads[String] = configFuncOf(ConfigValueType.STRING, _.getString(_))

  implicit val integer: HoconReads[Int] = configFuncOf(ConfigValueType.NUMBER, _.getInt(_))

  implicit val double: HoconReads[Double] = configFuncOf(ConfigValueType.NUMBER, _.getDouble(_))

  implicit val number: HoconReads[Number] = configFuncOf(ConfigValueType.NUMBER, _.getNumber(_))

  implicit val long: HoconReads[Long] = configFuncOf(ConfigValueType.NUMBER, _.getLong(_))

  implicit val boolean: HoconReads[Boolean] = configFuncOf(ConfigValueType.BOOLEAN, _.getBoolean(_))

  implicit val entityId: HoconReads[EntityId] = HoconReads.of((_,_) => HoconSuccess(NotStoredId))

  implicit val intProp: HoconReads[IntProp] = integer.map(IntProp.apply)

  implicit val doubleProp: HoconReads[DoubleProp] = double.map(DoubleProp.apply)

  def checkUnrecognizedField(obj: ConfigObject, meta: Meta): HoconResult[Unit] = {
    meta match {
      case m: EntityMeta =>
        val unrecognized = (obj.keySet() -- m.entityAliases)
        if (unrecognized.isEmpty)
          HoconSuccess(())
        else {
          HoconError.UnrecognizedFields(obj.origin(), unrecognized, m)
        }
      case els =>
        HoconError.TypeMismatched.of(obj.origin(), s"OBJECT type is not acceptable.", meta)
    }
  }

  implicit def array[T](implicit r: HoconReads[T]): HoconReads[Seq[T]] = HoconReads.of { (c, m) =>
    getByMetas(c, m, (conf, meta, alias) => {
      withConfigException2 {
        meta match {
          case am: ArrayMeta =>
            val list = conf.getList(alias)
            HoconResult.sequence(list) { value =>
              r.fromValue(value, meta).orElse {
                value.valueType() match {
                  case ConfigValueType.OBJECT =>
                    am.elements.map { objMeta =>
                      val confObj = value.asInstanceOf[ConfigObject]
                      checkUnrecognizedField(confObj, objMeta).flatMap(_ =>
                        r.read(confObj.toConfig, objMeta)
                      )
                    }.getOrElse(
                      HoconError.TypeMismatched.of(value.origin(), s"OBJECT type is not acceptable.", meta)
                    )
                  case ConfigValueType.NULL =>
                    HoconError.NotAcceptableValue(conf.origin(), "null", meta)
                  case els =>
                    sys.error(s"Not implemented for array of ${els} reads.")
                }
              }
            }
          case _ =>
            sys.error(s"Invalid meta for array: meta=${meta}, alias=${alias}")
        }
      }
    })
  }

  /**
    * Instance for Option has been delivered automatically
    * so we need explicitly import this one to override it before using HoconReads
    * @return
    */
  implicit def option[T](implicit tr: HoconReads[T]): HoconReads[Option[T]] = HoconReads.of[Option[T]]((conf,meta) => {
    tr.read(conf, meta) match {
      case HoconSuccess(t) => HoconSuccess(Some(t))
      case _: HoconError.NotExist => HoconSuccess(None)
      case e: HoconError => e
    }
  })
}

object HoconReadsCompanion extends LabelledTypeClassCompanion[HoconReads] {

  override object typeClass extends LabelledTypeClass[HoconReads] {
    override val emptyProduct: HoconReads[HNil] = HoconReads.of((_,_) => HoconSuccess(HNil))

    private[this] def resolveEntityMeta(propName: String, meta: Meta, conf: Config): HoconResult[(Config, Meta)] = {
      def getObjConfByMeta(conf: Config, targetMeta: EntityMeta): HoconResult[Config] = {
        HoconReads.getByMetas(conf, targetMeta, (c, m, alias) =>
          c.getValue(alias).valueType() match {
            case ConfigValueType.OBJECT =>
              val confObj = conf.getObject(alias)
              HoconReads.checkUnrecognizedField(confObj, targetMeta).map(_ => confObj.toConfig())
            case other =>
              HoconError.TypeMismatched.of(conf.origin(), s"has type $other, but required OBJECT", m)
          }
        ) match {
          case _: HoconError.NotExist if !targetMeta.required =>
            HoconResult(conf)
          case els => els
        }
      }

      meta match {
        case m: EntityMeta =>
          val propMeta = m.findByName(propName).getOrElse(
            sys.error(s"Meta of '${propName}' is not defined in meta=${m.entity.map(_.name)}"))
          propMeta match {
            case sub: EntityMeta =>
              getObjConfByMeta(conf, sub).map((_, sub))
            case els =>
              HoconResult(conf, propMeta)
          }
        case els =>
          HoconResult((conf, els))
      }
    }

    override def product[H, T <: HList](name: String, ch: HoconReads[H], ct: HoconReads[T]): HoconReads[::[H, T]] = {
      HoconReads.of[H :: T] { (conf, meta) =>
        val headParams = resolveEntityMeta(name, meta, conf)
        for {
          param <- headParams
          head <- ch.read(param._1, param._2)
          tail <- ct.read(conf, meta)
        } yield {
          head :: tail
        }
      }
    }

    private[this] def checkEnumConcordant[T](conf: Config, meta: EnumMeta)(candidate: T)(implicit strR: HoconReads[String]): HoconResult[T] = {
      candidate match {
        case _: Inl[_,_] => HoconSuccess(candidate)
        case _: Inr[_,_] => HoconSuccess(candidate)
        case _ =>
          strR.read(conf, meta).flatMap { actual =>
            val found = meta.values.find(_ == actual)
            found match {
              case Some(v) if v.toString == candidate.toString =>
                HoconSuccess(candidate)
              case _ =>
                HoconError.NotAcceptableValue(conf.origin(), actual, meta)
            }
          }
      }
    }

    override def coproduct[L, R <: Coproduct](name : String, cl : => HoconReads[L], cr : => HoconReads[R]) : HoconReads[L :+: R] = {
      HoconReads.of[L :+: R] { (conf, meta) =>
        meta match {
          case enumMeta: EnumMeta =>
            cr.read(conf, enumMeta).flatMap(checkEnumConcordant(conf, enumMeta)).map(Inr(_))
            .orElse{
              cl.read(conf, enumMeta).flatMap(checkEnumConcordant(conf, enumMeta)).map(Inl(_))
            }
          case els =>
            cr.read(conf, meta).map(Inr(_)).orElse(cl.read(conf, meta).map(Inl(_)))
        }
      }
    }

    override val emptyCoproduct: HoconReads[CNil] = HoconReads.of((c, m) =>
      HoconError.UnknownError(c.origin(), "Unexpected coproduct value."))

    override def project[F, G](instance: => HoconReads[G], to: (F) => G, from: (G) => F): HoconReads[F] =
      HoconReads.of(instance.read(_,_).map(from))
  }
}
