package org.jetbrains.plugins.scala.failed.typeInference

import org.jetbrains.plugins.scala.PerfCycleTests
import org.jetbrains.plugins.scala.lang.typeInference.TypeInferenceTestBase
import org.junit.experimental.categories.Category

/**
  * Created by kate on 4/1/16.
  */

@Category(Array(classOf[PerfCycleTests]))
class JavaConstructorParameters extends TypeInferenceTestBase{
  override def folderPath: String = super.folderPath + "bugs5/"

  def testSCL9875(): Unit = doTest()
}
