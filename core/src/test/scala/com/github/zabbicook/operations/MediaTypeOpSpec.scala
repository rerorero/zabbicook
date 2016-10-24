package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.media.{MediaType, MediaTypeType}
import com.github.zabbicook.test.{TestMedia, UnitSpec}

class MediaTypeOpSpec extends UnitSpec with TestMedia {
  lazy val sut = testMediaTypeOp

  "present mediatypes" should "create, delete, and update mediatypes" in {
    val added = MediaType[NotStored](
      description = specName("mediatype X"),
      `type` = MediaTypeType.Jabber,
      username = Some("user@server"),
      passwd = Some("password"),
      status = true
    )

    def clean() = {
      await(sut.absent(Seq(added.description)))
      cleanTestMediaTypes()
    }
    cleanRun(clean) {
      assert(Seq() === await(sut.findByDescriptions(testMediaTypes.map(_.description))))

      def check(conf: Seq[MediaType[NotStored]]): Unit = {
        val actuals = await(sut.findByDescriptions(conf.map(_.description)))
        assert(conf.length === actuals.length)
        conf.map { expected =>
          val Some(actual) = actuals.find(_.description == expected.description)
          assert(false === actual.shouldBeUpdated(expected))
        }
      }

      // creates
      {
        presentTestMediaTypes()
        check(testMediaTypes)
        // represent does nothing
        val report = await(sut.present(testMediaTypes))
        assert(report.isEmpty())
      }

      // append
      {
        val appended = testMediaTypes :+ added
        val r = await(sut.present(appended))
        assert(1 === r.count)
        assert(added.entityName === r.created.head.entityName)
        check(appended)
        // represent does nothing
        val report2 = await(sut.present(appended))
        assert(report2.isEmpty())
      }

      // update
      {
        val updated = testMediaTypes :+ added.copy(
          username = Some("updated@server"),
          passwd = Some("pass")
        )
        val r = await(sut.present(updated))
        assert(1 === r.count)
        assert(added.entityName === r.updated.head.entityName)
        check(updated)
        // represent does nothing
        val report2 = await(sut.present(updated))
        assert(report2.isEmpty())
      }
    }
  }
}
