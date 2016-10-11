package com.github.zabbicook.hocon

import java.io.File

import com.github.zabbicook.entity.EntityId.NotStoredId
import com.github.zabbicook.entity._
import com.github.zabbicook.entity.prop._
import com.typesafe.config._
import shapeless._

import scala.collection.JavaConversions._

object HoconReader2 {
  def read[T](v: => Config, meta: Meta)(implicit reader: HoconReads2[T]): HoconResult[T] = {
    HoconReads2.withConfigException2(reader.read(v, meta))
  }

  def read[T](file: File, meta: Meta)(implicit reader: HoconReads2[T]): HoconResult[T] = read(ConfigFactory.parseFile(file), meta)

  def read[T](s: String, meta: Meta)(implicit reader: HoconReads2[T]): HoconResult[T] = read(ConfigFactory.parseString(s), meta)
}

trait HoconReads2[T] { self =>
  def read(o: Config, meta: Meta): HoconResult[T]

  def fromValue(v: ConfigValue, meta: Meta): HoconResult[T] = {
    HoconError.UnknownError(v.origin(), s"fromValue function has not been implemented. ${v.valueType()} meta=${meta}")
  }

  def map[U](f: T => U): HoconReads2[U] = HoconReads2.of[U]((conf, meta) =>
    self.read(conf, meta).map(f)
  )

  def flatMap[U](f: T => HoconReads2[U]): HoconReads2[U] = HoconReads2.of[U]((conf, meta) =>
    self.read(conf, meta).flatMap(f(_).read(conf, meta))
  )
}

object HoconReads2 {
  def apply[T](implicit reads: HoconReads2[T]) = reads

  def of[T](f: (Config, Meta) => HoconResult[T]): HoconReads2[T] = new HoconReads2[T] {
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
      case None =>
        println("-- natoring -- not exist", meta.aliases)
        println("-- natoring -- ", conf)
        HoconError.NotExist(conf.origin(), meta)
    }
  }

  private[this] def configFuncOf[T](expectType: ConfigValueType, f: (Config, String) => T): HoconReads2[T] = {
    new HoconReads2[T] {
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

  implicit val string: HoconReads2[String] = configFuncOf(ConfigValueType.STRING, _.getString(_))

  implicit val integer: HoconReads2[Int] = configFuncOf(ConfigValueType.NUMBER, _.getInt(_))

  implicit val double: HoconReads2[Double] = configFuncOf(ConfigValueType.NUMBER, _.getDouble(_))

  implicit val number: HoconReads2[Number] = configFuncOf(ConfigValueType.NUMBER, _.getNumber(_))

  implicit val long: HoconReads2[Long] = configFuncOf(ConfigValueType.NUMBER, _.getLong(_))

  implicit val boolean: HoconReads2[Boolean] = configFuncOf(ConfigValueType.BOOLEAN, _.getBoolean(_))

  implicit val entityId: HoconReads2[EntityId] = HoconReads2.of((_,_) => HoconSuccess(NotStoredId))

  implicit val intProp: HoconReads2[IntProp] = integer.map(IntProp.apply)

  implicit val doubleProp: HoconReads2[DoubleProp] = double.map(DoubleProp.apply)

  implicit def array[T](implicit r: HoconReads2[T]): HoconReads2[Seq[T]] = HoconReads2.of { (c, m) =>
    getByMetas(c, m, (conf, meta, alias) => {
      withConfigException2 {
        meta match {
          case am: ArrayMeta =>
            val list = conf.getList(alias)
            HoconResult.sequence(list) { value =>
              r.fromValue(value, meta).orElse {
                value.valueType() match {
                  case ConfigValueType.OBJECT =>
                    am.elements.map( objMeta =>
                      r.read(value.asInstanceOf[ConfigObject].toConfig, objMeta)
                    ).getOrElse(HoconError.TypeMismatched.of(value.origin(), "has type OBJECT but required OBJECT", meta))
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
  implicit def option[T](implicit tr: HoconReads2[T]): HoconReads2[Option[T]] = HoconReads2.of[Option[T]]((conf,meta) => {
    tr.read(conf, meta) match {
      case HoconSuccess(t) => HoconSuccess(Some(t))
      case _: HoconError.NotExist =>
        println(" -- option natoring -- NotExist", meta.aliases)
        println(" -- option natoring--", conf)
        HoconSuccess(None)
      case e: HoconError => e
    }
  })
}

object HoconReadsCompanion extends LabelledTypeClassCompanion[HoconReads2] {

  override object typeClass extends LabelledTypeClass[HoconReads2] {
    override val emptyProduct: HoconReads2[HNil] = HoconReads2.of((_,_) => HoconSuccess(HNil))

    private[this] def resolveEntityMeta(propName: String, meta: Meta, conf: Config): HoconResult[(Config, Meta)] = {
      def getObjConfByMeta(conf: Config, targetMeta: EntityMeta, entityMeta: Option[EntityMeta] = None): HoconResult[Config] = {
        val parent = entityMeta.getOrElse(targetMeta)
        HoconReads2.getByMetas(conf, targetMeta, (c, m, alias) =>
          c.getValue(alias).valueType() match {
            case ConfigValueType.OBJECT =>
              val confObj = conf.getObject(alias)
              val unrecognized = (confObj.keySet() -- parent.entityAliases)
              if (unrecognized.isEmpty)
                HoconResult(confObj.toConfig())
              else {
                HoconError.UnrecognizedFields(confObj.origin(), unrecognized, parent)
              }
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

    override def product[H, T <: HList](name: String, ch: HoconReads2[H], ct: HoconReads2[T]): HoconReads2[::[H, T]] = {
      HoconReads2.of[H :: T] { (conf, meta) =>
        println("natoring1", name, meta)
        val headParams = resolveEntityMeta(name, meta, conf)
        for {
          param <- headParams
          _ = println("natoring2", name, headParams)
          head <- ch.read(param._1, param._2)
          tail <- ct.read(conf, meta)
        } yield {
          head :: tail
        }
      }
    }

    private[this] def checkEnumConcordant[T](conf: Config, meta: EnumMeta)(candidate: T)(implicit strR: HoconReads2[String]): HoconResult[T] = {
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

    override def coproduct[L, R <: Coproduct](name : String, cl : => HoconReads2[L], cr : => HoconReads2[R]) : HoconReads2[L :+: R] = {
      HoconReads2.of[L :+: R] { (conf, meta) =>
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

    override val emptyCoproduct: HoconReads2[CNil] = HoconReads2.of((c, m) =>
      HoconError.UnknownError(c.origin(), "Unexpected coproduct value."))

    override def project[F, G](instance: => HoconReads2[G], to: (F) => G, from: (G) => F): HoconReads2[F] =
      HoconReads2.of(instance.read(_,_).map(from))
  }
}
