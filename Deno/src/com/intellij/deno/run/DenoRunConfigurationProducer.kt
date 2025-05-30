package com.intellij.deno.run

import com.intellij.deno.isDenoEnableForContext
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.util.ScriptFileUtil
import com.intellij.javascript.debugger.execution.DebuggableProcessRunConfigurationBase
import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.ecmascript6.TypeScriptUtil
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

internal class DenoRunConfigurationProducer : LazyRunConfigurationProducer<DenoRunConfiguration>() {

  private val ConfigurationContext?.isAcceptable: Boolean
    get() {
      val original = this?.getOriginalConfiguration(null)
      return original == null || original is DenoRunConfiguration
    }

  override fun getConfigurationFactory() = runConfigurationType<DenoConfigurationType>()

  override fun setupConfigurationFromContext(configuration: DenoRunConfiguration,
                                             context: ConfigurationContext,
                                             sourceElement: Ref<PsiElement>): Boolean {
    val psiFile = sourceElement.get()?.containingFile ?: return false
    if (!isDenoEnableForContext(psiFile) || !context.isAcceptable) {
      return false
    }

    val virtualFile = psiFile.virtualFile ?: return false
    val project = psiFile.project

    if (!isAcceptableFileType(virtualFile) || !virtualFile.isInLocalFileSystem) return false

    if (StringUtil.isEmpty(configuration.workingDirectory)) {
      val parentDir: VirtualFile? = virtualFile.parent
      configuration.workingDirectory = if (parentDir != null && parentDir.isInLocalFileSystem) {
        parentDir.path
      }
      else {
        (project.guessProjectDir() ?: parentDir)?.path
      }
    }
    configuration.inputPath = virtualFile.path
    val nameWithoutExt = virtualFile.nameWithoutExtension
    if (nameWithoutExt.endsWith(".test") || nameWithoutExt.endsWith("_test") || nameWithoutExt == "test") {
      configuration.programParameters = "test"
    }

    configuration.setGeneratedName()

    return true
  }

  private fun isAcceptableFileType(virtualFile: VirtualFile): Boolean {
    val fileType = virtualFile.fileType
    return TypeScriptUtil.isTypeScriptFile(virtualFile) || fileType == JavaScriptFileType
  }

  override fun isConfigurationFromContext(configuration: DenoRunConfiguration, context: ConfigurationContext): Boolean {
    if (!context.isAcceptable) return false

    val location = context.location ?: return false
    val psiElement = location.psiElement
    if (!isDenoEnableForContext(psiElement)) return false

    val file = location.virtualFile ?: return false

    return ScriptFileUtil.getScriptFilePath(file) == configuration.inputPath ||
           file == DebuggableProcessRunConfigurationBase.findInputVirtualFile(configuration)
  }
}