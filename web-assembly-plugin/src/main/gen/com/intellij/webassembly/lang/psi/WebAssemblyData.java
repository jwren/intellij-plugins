// This is a generated file. Not intended for manual editing.
package com.intellij.webassembly.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

public interface WebAssemblyData extends WebAssemblyNamedReferencedElement {

  @Nullable
  WebAssemblyIdx getIdx();

  @NotNull
  List<WebAssemblyInstr> getInstrList();

  @Nullable
  PsiElement setName(@NotNull String name);

  @Nullable
  PsiElement getNameIdentifier();

  @Nullable
  PsiReference getReference();

  @NotNull
  PsiReference[] getReferences();

}
