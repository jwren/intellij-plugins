import type * as ts from "tsc-ide-plugin/tsserverlibrary.shim"
import type {GetElementTypeResponse} from "tsc-ide-plugin/protocol"

declare module "tsc-ide-plugin/tsserverlibrary.shim" {

  interface LanguageService {
    webStormNgGetGeneratedElementType(
      ts: typeof import("tsc-ide-plugin/tsserverlibrary.shim"),
      fileName: string,
      range: {
        start: ts.LineAndCharacter;
        end: ts.LineAndCharacter;
      },
      forceReturnType: boolean,
      cancellationToken: import("tsc-ide-plugin/tsserverlibrary.shim").CancellationToken,
    ): GetElementTypeResponse | undefined
  }
}

export = ts;