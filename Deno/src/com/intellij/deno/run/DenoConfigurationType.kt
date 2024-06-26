package com.intellij.deno.run

import com.intellij.deno.DenoBundle
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.javascript.debugger.JavaScriptDebuggerIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class DenoConfigurationType :
  SimpleConfigurationType("DenoConfigurationType", DenoBundle.message("deno.name"), DenoBundle.message("deno.run.configuration.description"), NotNullLazyValue.createConstantValue(
    JavaScriptDebuggerIcons.JavaScript_debug_configuration)), DumbAware {

  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return DenoRunConfiguration(project, this, "Deno")
  }
}