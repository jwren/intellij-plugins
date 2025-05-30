package org.jetbrains.plugins.cucumber.java.steps.reference;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.plugins.cucumber.java.CucumberJavaUtil.*;

public final class CucumberJavaImplicitUsageProvider implements ImplicitUsageProvider {
  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    if (element instanceof PsiClass psiClass) {
      return isStepDefinitionClass(psiClass);
    }
    else if (element instanceof PsiMethod method) {
      return isStepDefinition(method) || isHook(method) || isParameterType(method);
    }

    return false;
  }

  @Override
  public boolean isImplicitRead(@NotNull PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(@NotNull PsiElement element) {
    return false;
  }
}
