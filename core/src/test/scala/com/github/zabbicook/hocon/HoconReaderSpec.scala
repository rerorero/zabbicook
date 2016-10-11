package com.github.zabbicook.hocon

import com.github.zabbicook.entity.prop._
import com.github.zabbicook.test.UnitSpec
import com.typesafe.config.ConfigFactory

class HoconReaderSpec extends UnitSpec {

  import HoconReadsCompanion._

  import HoconReads.option

  sealed abstract class Address(val zabbixValue: String, val desc: String) extends EnumProp2[String]

  object Address extends StringEnumProp2Companion[Address] {
    override val values: Set[Address] = Set(earth,other)
    override val description: String = "Address"
    case object earth extends Address("earth", "it is an earth")
    case object other extends Address("universe", "an universe")
    case object unknown extends Address("unknown", "??")
  }

  sealed abstract class Income(val zabbixValue: IntProp, val desc: String) extends EnumProp2[IntProp]

  object Income extends IntEnumProp2Companion[Income] {
    override val values: Set[Income] = Set(millionaire, poor)
    override val description: String = "Incomes"
    case object millionaire extends Income(0, "who is a millionaire")
    case object poor extends Income(1, "poooor")
    case object unknown extends Income(-1, "unknown")
  }

  case class Person(
    name: String,
    age: Option[Int] = None,
    address: Option[Address] = None,
    income: Option[Income] = None,
    factor: Option[DoubleProp] = None
  )
  object Person extends EntityCompanionMetaHelper {
    import Meta._
    val meta = entity("test persons")(
      value("name")("name")("whose name"),
      value("age")("ageee", "age")("whose age"),
      Address.meta("address")("address"),
      Income.meta("income")("in", "income"),
      value("factor")("factor")("What is this?")
    ) _
  }

  case class World(me: Person, others: Seq[Person], god: Option[Person])

  object World extends EntityCompanionMetaHelper {
    import Meta._
    val meta = entity("Hole world")(
      Person.required("me"),
      arrayOf("others")(Person.required("others")),
      Person.optional("god")
    ) _
  }

  "reader" should "reads hocon to some objects" in {
    val alice = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{ name: "Alice", age: 12 , factor: 0.01 }"""),
      Person.optional("dummy")
    )
    val bob = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{name="Bob"}"""),
      Person.optional("dummy")
    )
    assert(alice === HoconSuccess(Person("Alice",Some(12), factor=Some(DoubleProp(0.01)))))
    assert(bob === HoconSuccess(Person("Bob",None)))
  }

  "reader" should "reads hocon to some objects includes Array" in {
    val s =
      """
        |{
        |  me: { name: "Alice", age: 10, address: "earth" }
        |  others: [
        |    { name: "Bob", income: "millionaire" }
        |    { name: "Charlie", age: 99 }
        |  ]
        |  god: { name: "Yes", age: 2016, address: "other", income: "poor" }
        |}
      """.stripMargin
    val world = HoconReader.read[World](
      ConfigFactory.parseString(s),
      World.optional("dummy")
    )
    assert(world === HoconSuccess(World(
      me = Person("Alice", Some(10), Some(Address.earth)),
      others = Seq(Person("Bob", income = Some(Income.millionaire)), Person("Charlie", Some(99))),
      god = Some(Person("Yes", Some(2016), Some(Address.other), Some(Income.poor)))
    )))
  }

  "read" should "fail when types are mismatched" in {
    val r = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{ name: "Alice", age: "hoge"}"""),
      Person.optional("dummy")
    )
    assert(r.isInstanceOf[HoconError.TypeMismatched])
  }

  "read" should "fail when required props do not exist" in {
    val r = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{age: 1}"""),
      Person.optional("dummy")
    )
    assert(r.isInstanceOf[HoconError.NotExist])
  }

  "read" should "fail when inputs are invalid format" in {
    val r = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{age = 1"""),
      Person.optional("dummy")
    )
    assert(r.isInstanceOf[HoconError.ParseFailed])
  }

  "read" should "fail when includes invalid enum string" in {
    val r = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{name: "Alice", address: "not applicative address"}"""),
      Person.optional("dummy")
    )
    assert(r.isInstanceOf[HoconError.NotAcceptableValue])
    assert(r.asInstanceOf[HoconError.NotAcceptableValue].value === "not applicative address")
    assert(r.asInstanceOf[HoconError.NotAcceptableValue].meta.aliases === Set("address"))

    val r2 = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{name: "Alice", income: "not applicative address"}"""),
      Person.optional("dummy")
    )
    assert(r2.isInstanceOf[HoconError.NotAcceptableValue])
    assert(r2.asInstanceOf[HoconError.NotAcceptableValue].value === "not applicative address")
    assert(r2.asInstanceOf[HoconError.NotAcceptableValue].meta.aliases === Set("in", "income"))
  }

  "read" should "fail with unrecognized keys" in {
    val s =
      """
        |{
        |  me: { name: "Alice", ages: 10, hooo: "earth" }
        |  others: []
        |}
      """.stripMargin
    val r = HoconReader.read[World](ConfigFactory.parseString(s), World.optional("dummy"))
    assert(r.isInstanceOf[HoconError.UnrecognizedFields])
    assert(r.asInstanceOf[HoconError.UnrecognizedFields].invalids === Set("ages", "hooo"))
  }

  case class PersonAdditional(
    favorite: Option[String],
    nickName: String
  )
  object PersonAdditional extends EntityCompanionMetaHelper {
    import Meta._
    def meta = entity("additional")(
      value("favorite")("favorite")("favorite foods"),
      value("nickName")("nickName")("Nick name")
    )
  }
}
