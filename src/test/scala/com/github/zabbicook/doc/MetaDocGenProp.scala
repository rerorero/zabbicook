package com.github.zabbicook.doc

import com.github.zabbicook.recipe.Recipe
import com.github.zabbicook.test.UnitSpec

import scala.util.Success

class MetaDocGenProp extends UnitSpec {
  "genBuilder" should "generate string builder" in {
    val meta = Recipe.required("")

    {
      val sb = MetaDocGen.genBuilder(meta)
      val str = sb.toString()
      assert(str.contains("templates"))
      assert(str.contains("color"))
    }
    {
      val sb = MetaDocGen.genBuilder(meta, maxDepth = 2)
      val str = sb.toString()
      assert(str.contains("templates"))
      assert(false === str.contains("color"))
    }
  }

  "pathOf" should "success in correct path" in {
    val meta = Recipe.required("(root)")
    val Success(sb) = MetaDocGen.pathOf("templates.graphs.items", meta)
    val str = sb.toString()
    assert(str.split(System.lineSeparator()).head.contains("items"))
    assert(str.contains("color"))
  }
}
