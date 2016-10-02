package com.github.zabbicook.entity

import com.github.zabbicook.entity.ValidationResult.{NotAcceptable, Valid}

trait EnumProp {
  def validate(): ValidationResult
}

trait EnumCompanion[T] {

  def all: Set[T]

  def unknown: T

  def validate(value: T): ValidationResult = {
    val acceptable = all - unknown
    if (acceptable.contains(value))
      Valid
    else
      NotAcceptable(acceptable.map(_.toString))
  }
}
