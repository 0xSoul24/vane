package org.oddlama.vane.annotation.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedAnnotationTypes("org.oddlama.vane.annotation.VaneModule")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class VaneModuleProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        annotations.forEach { annotation ->
            val elements = roundEnv.getElementsAnnotatedWith(annotation)
            elements.forEach { verifyIsClass(processingEnv, it, "VaneModule") }
            elements.forEach { verifyExtendsType(processingEnv, it, "org.oddlama.vane.core.module.Module<", "VaneModule", "org.oddlama.vane.core.Module") }
        }

        return true
    }
}
