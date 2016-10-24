package com.github.zabbicook.test

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.media.{MediaType, MediaTypeType}
import com.github.zabbicook.operation.MediaTypeOp

trait TestMedia extends TestConfig { self: UnitSpec =>
  protected[this] lazy val testMediaTypeOp = new MediaTypeOp(cachedApi)

  /**
    * you can override to customize.
    */
  protected[this] val testMediaTypes: Seq[MediaType[NotStored]] = Seq(
    MediaType(
      description = specName("MediaType1"),
      `type` = MediaTypeType.email,
      smtp_email = Some("test@example.com"),
      smtp_helo = Some("zabbicook.example.com"),
      smtp_server = Some("exapmle.com"),
      status = true
    ),
    MediaType(
      description = specName("MediaType2"),
      `type` = MediaTypeType.script,
      exec_path = Some("/usr/local/bin/test.sh"),
      exec_params = Some("""{ALERT.SENDTO}
                           |{ALERT.SUBJECT}
                           |{ALERT.MESSAGE}
                           |""".stripMargin),
      status = true
    ),
    MediaType(
      description = specName("MediaType3"),
      `type` = MediaTypeType.SMS,
      gsm_modem = Some("modem"),
      status = false
    )
  )

  def presentTestMediaTypes(): Unit = {
    await(testMediaTypeOp.present(testMediaTypes))
  }

  def cleanTestMediaTypes(): Unit = {
    await(testMediaTypeOp.absent(testMediaTypes.map(_.description)))
  }
}
