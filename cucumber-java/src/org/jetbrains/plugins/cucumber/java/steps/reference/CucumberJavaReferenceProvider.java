// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.cucumber.java.steps.reference;

import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.manipulators.StringLiteralManipulator;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.CucumberUtil;
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil;

import java.util.ArrayList;
import java.util.List;

public final class CucumberJavaReferenceProvider extends PsiReferenceProvider {
  @Override
  public boolean acceptsTarget(@NotNull PsiElement target) {
    return target instanceof PomTargetPsiElement pomTarget && pomTarget.getTarget() instanceof CucumberJavaParameterPomTarget;
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    if (!(element instanceof PsiLiteralExpression literalExpression)) {
      return PsiReference.EMPTY_ARRAY;
    }
    final Object value = literalExpression.getValue();
    if (!(value instanceof String)) {
      return PsiReference.EMPTY_ARRAY;
    }

    final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
    if (annotation == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    if (!CucumberJavaUtil.isCucumberStepAnnotation(annotation)) {
      return PsiReference.EMPTY_ARRAY;
    }
    final String cucumberExpression = CucumberJavaUtil.getAnnotationValue(annotation);
    if (cucumberExpression == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    final List<CucumberJavaParameterTypeReference> result = new ArrayList<>();
    CucumberUtil.processParameterTypesInCucumberExpression(literalExpression.getValue().toString(), range -> {
      // Skip " at the beginning of the string literal
      range = range.shiftRight(StringLiteralManipulator.getValueRange(literalExpression).getStartOffset());
      result.add(new CucumberJavaParameterTypeReference(element, range));
      return true;
    });
    return result.toArray(new CucumberJavaParameterTypeReference[0]);
  }
}
