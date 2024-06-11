package org.angular2.lang.html.tcb

import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.javascript.psi.JSElementVisitor
import com.intellij.lang.javascript.psi.ecma6.TypeScriptClass
import com.intellij.lang.typescript.compiler.TypeScriptCompilerConfigUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.SmartList
import org.angular2.Angular2DecoratorUtil
import org.angular2.entities.Angular2EntitiesProvider
import org.angular2.lang.html.tcb.Angular2TemplateTranspiler.SourceMapping
import org.angular2.lang.html.tcb.Angular2TemplateTranspiler.TranspiledTemplate

object Angular2TranspiledComponentFileBuilder {

  fun buildTranspiledComponentFile(componentFile: PsiFile): TranspiledComponentFile? {
    val cache = getComponentFileCache(componentFile) ?: return null
    return buildTranspiledComponentFile(componentFile, cache)
  }

  private fun buildTranspiledComponentFile(componentFile: PsiFile, cache: ComponentFileCache): TranspiledComponentFile {
    val templates = cache.components.mapIndexedNotNull { index, cls ->
      CachedValuesManager.getCachedValue(cls) {
        CachedValueProvider.Result.create(Angular2EntitiesProvider.getComponent(cls)?.let {
          Angular2TemplateTranspiler.transpileTemplate(it, index.toString())
        }, PsiModificationTracker.MODIFICATION_COUNT)
      }
    }
    return buildTranspiledComponentFile(componentFile, templates)
  }

  private fun getComponentFileCache(file: PsiFile): ComponentFileCache? =
    CachedValuesManager.getCachedValue(file) {
      val result = SmartList<TypeScriptClass>()
      file.acceptChildren(object : JSElementVisitor() {
        override fun visitTypeScriptClass(typeScriptClass: TypeScriptClass) {
          if (Angular2DecoratorUtil.findDecorator(typeScriptClass, Angular2DecoratorUtil.COMPONENT_DEC) != null)
            result.add(typeScriptClass)
        }

        override fun visitES6ExportDefaultAssignment(node: ES6ExportDefaultAssignment) {
          node.acceptChildren(this)
        }
      })
      CachedValueProvider.Result.create(if (result.isEmpty()) null else ComponentFileCache(result), file)
    }


  private class ComponentFileCache(val components: List<TypeScriptClass>)

  private fun buildTranspiledComponentFile(componentFile: PsiFile, templates: List<TranspiledTemplate>): TranspiledComponentFile {
    val generatedCode = StringBuilder()
    val componentFileText = componentFile.text
    generatedCode.append(componentFileText)

    val injectedLanguageManager = InjectedLanguageManager.getInstance(componentFile.project)
    val inlineTemplateRanges = SmartList<TextRange>()

    val componentFileMappings = SmartList<SourceMapping>()
    val mappings = SmartList<FileMappings>()

    generatedCode.append("\n\n/* Additional imports */\n\n")
    for (template in templates) {
      // TODO add imports
    }

    for (template in templates) {
      generatedCode.append("\n\n/* TCB for ")
        .append(template.templateFile.name)
        .append(" */\n\n")
      val generatedMappingsOffset = generatedCode.length
      generatedCode.append(template.generatedCode)

      val injectionHost = injectedLanguageManager.getInjectionHost(template.templateFile)
      if (injectionHost != null) {
        val hostRange = injectionHost.textRange
        inlineTemplateRanges.add(hostRange)
        val sourceMappingOffset = hostRange.startOffset + 1
        template.sourceMappings.mapTo(componentFileMappings) {
          it.offsetBy(sourceOffset = sourceMappingOffset, generatedOffset = generatedMappingsOffset)
        }
      }
      else {
        val fileMappings = template.sourceMappings.map {
          it.offsetBy(generatedOffset = generatedMappingsOffset)
        }
        mappings.add(FileMappings(template.templateFile, fileMappings))
      }
    }
    inlineTemplateRanges.sortBy { it.startOffset }
    var lastRangeEnd = 0
    for (inlineTemplateRange in inlineTemplateRanges + TextRange(componentFileText.length, componentFileText.length)) {
      componentFileMappings.add(SourceMappingData(
        lastRangeEnd,
        inlineTemplateRange.startOffset - lastRangeEnd,
        lastRangeEnd,
        inlineTemplateRange.startOffset - lastRangeEnd,
        false
      ))
      lastRangeEnd = inlineTemplateRange.endOffset
    }

    mappings.add(FileMappings(componentFile, componentFileMappings))
    return TranspiledComponentFile(
      generatedCode.toString(),
      mappings
    )
  }

  data class TranspiledComponentFile(
    val generatedCode: String,
    val mappings: List<FileMappings>,
  )

  class FileMappings(
    val sourceFile: PsiFile,
    val sourceMappings: List<SourceMapping>,
  ) {
    val fileName = this.sourceFile.viewProvider.virtualFile.let { TypeScriptCompilerConfigUtil.normalizeNameAndPath(it) }
                   ?: "<non-local>"
  }

}