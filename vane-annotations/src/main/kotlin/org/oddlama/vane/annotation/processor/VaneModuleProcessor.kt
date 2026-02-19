package org.oddlama.vane.annotation.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.tools.Diagnostic

@SupportedAnnotationTypes("org.oddlama.vane.annotation.VaneModule")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class VaneModuleProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement?>, roundEnv: RoundEnvironment): Boolean {
        for (annotation in annotations) {
            roundEnv.getElementsAnnotatedWith(annotation).forEach { element: Element -> verifyIsClass(processingEnv, element, "VaneModule") }
            roundEnv.getElementsAnnotatedWith(annotation)
                .forEach { element: Element? -> verifyExtendsType(processingEnv, element, "org.oddlama.vane.core.module.Module<", "VaneModule", "org.oddlama.vane.core.Module") }
        }

        return true
    }
}
