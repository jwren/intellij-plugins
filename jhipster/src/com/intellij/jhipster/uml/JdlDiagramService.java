// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.jhipster.uml;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;

@Service(Service.Level.PROJECT)
public final class JdlDiagramService implements Disposable {
  @Override
  public void dispose() {
  }
}
