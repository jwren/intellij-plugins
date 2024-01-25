/*
 * Copyright (c) 2024, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.LSPDartTextDocumentContentConsumer;
import com.google.gson.JsonObject;

import org.dartlang.analysis.server.protocol.RequestError;

/**
 * Instances of {@code LSPDartTextDocumentContentProcessor} translate JSON result objects for a given
 * {@link LSPDartTextDocumentContentConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class LSPDartTextDocumentContentProcessor extends ResultProcessor {

  private final LSPDartTextDocumentContentConsumer consumer;

  public LSPDartTextDocumentContentProcessor(LSPDartTextDocumentContentConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject, RequestError requestError) {
    if (resultObject != null) {
      System.out.println("resultObject = " + resultObject);
      //{"lspResponse":{"id":"10","jsonrpc":"2.0","result":{"content":"library augment 'test.dart';\n\naugment class X {\n  void foo() { /*comment232*/print(\"Hello!\"); }\n}\n"}}}
      System.out.println();
      JsonObject lspResponse = resultObject.getAsJsonObject("lspResponse");
      JsonObject innerResultObject = lspResponse.getAsJsonObject("result");
      final String contents = innerResultObject.get("content").getAsString();
      consumer.computedDocumentContents(contents);
    }
    if (requestError != null) {
      consumer.onError(requestError);
    }
  }
}
