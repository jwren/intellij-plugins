// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.index

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StringStubIndexExtension

abstract class Angular2IndexBase<T : PsiElement> : StringStubIndexExtension<T>() {
  override fun getVersion(): Int = 9
}

