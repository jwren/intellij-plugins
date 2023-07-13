// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.angular2.codeInsight

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.angular2.Angular2CodeInsightFixtureTestCase
import org.angular2.Angular2TestModule
import org.angularjs.AngularTestUtil

class Angular2CopyPasteTest : Angular2CodeInsightFixtureTestCase() {
  override fun getBasePath(): String = ""
  override fun getTestDataPath(): String {
    return AngularTestUtil.getBaseTestDataPath() + "copyPaste"
  }

  private fun doTest(srcExt: String, destExt: String) {
    myFixture.copyDirectoryToProject(getTestName(false), ".")
    Angular2TestModule.configureCopy(myFixture,
                                     Angular2TestModule.ANGULAR_CORE_13_3_5,
                                     Angular2TestModule.ANGULAR_COMMON_13_3_5,
                                     Angular2TestModule.ANGULAR_CDK_14_2_0)
    myFixture.configureFromTempProjectFile("source.component.$srcExt")
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_COPY)
    myFixture.configureFromTempProjectFile("destination.component.$destExt")
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_PASTE)
    WriteAction.runAndWait<Throwable> {
      FileDocumentManager.getInstance().saveAllDocuments()
    }
    myFixture.checkResultByFile(getTestName(false) + "/destination.component.$destExt.after")
    if (destExt != "ts") {
      myFixture.configureFromTempProjectFile("destination.component.ts")
      myFixture.checkResultByFile(getTestName(false) + "/destination.component.ts.after")
    }
    if (myFixture.tempDirFixture.getFile("destination.module.ts") != null) {
      myFixture.configureFromTempProjectFile("destination.module.ts")
      myFixture.checkResultByFile(getTestName(false) + "/destination.module.ts.after")
    }
  }

  fun testBasic() {
    doTest("html", "html")
  }

  fun testBasicToInjected() {
    doTest("html", "ts")
  }

  fun testInjected() {
    doTest("ts", "ts")
  }

  fun testInjectedToBasic() {
    doTest("ts", "html")
  }

  fun testExpression() {
    doTest("html", "html")
  }

  fun testExpressionFromInjected() {
    doTest("ts", "html")
  }

  fun testExpressionToInjected() {
    doTest("html", "ts")
  }

  fun testExpressionToHtml() {
    doTest("html", "html")
  }

  fun testHtmlToExpression() {
    doTest("html", "html")
  }

  fun testNgFor() {
    doTest("html", "html")
  }

}