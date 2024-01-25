// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.google.dart.server;

import org.dartlang.analysis.server.protocol.RequestError;

public interface LSPDartTextDocumentContentConsumer extends Consumer {

  public void computedDocumentContents(String contents);

  /**
   * If a search id cannot be passed back, some {@link RequestError} is passed back instead.
   *
   * @param requestError the reason why a result was not passed back
   */
  public void onError(RequestError requestError);
}
