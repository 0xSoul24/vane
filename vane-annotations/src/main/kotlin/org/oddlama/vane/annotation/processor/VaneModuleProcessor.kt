package org.oddlama.vane.annotation.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/** Validates usage of the `@VaneModule` annotation. */
@SupportedAnnotationTypes("org.oddlama.vane.annotation.VaneModule")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class VaneModuleProcessor : AbstractProcessor() {
    /**
     * Processes elements annotated with `@VaneModule` and validates their
     * conformance to being a proper module class in the framework.
     *
     * @param annotations The set of annotations to process.
     * @param roundEnv The processing environment for this round.
     * @return true to indicate annotations are claimed by this processor.
     */
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        annotations.forEach { annotation ->
            val elements = roundEnv.getElementsAnnotatedWith(annotation)
            elements.forEach {
                verifyIsClass(processingEnv, it, "VaneModule")
                verifyExtendsType(
                    processingEnv,
                    it,
                    "org.oddlama.vane.core.module.Module<",
                    "VaneModule",
                    "org.oddlama.vane.core.Module"
                )
            }
        }

        return true
    }
}
