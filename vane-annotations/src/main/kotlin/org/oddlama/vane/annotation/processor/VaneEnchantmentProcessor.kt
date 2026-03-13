package org.oddlama.vane.annotation.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/** Annotation processor that validates usages of `@VaneEnchantment`. */
@SupportedAnnotationTypes("org.oddlama.vane.annotation.enchantment.VaneEnchantment")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class VaneEnchantmentProcessor : AbstractProcessor() {
    /**
     * Processes all elements annotated with `@VaneEnchantment` for this round.
     *
     * Validates that each annotated element is a class and that it extends the
     * expected framework base enchantment type.
     *
     * @param annotations The set of annotation types requested to be processed.
     * @param roundEnv Environment for information about the current and prior round.
     * @return true to indicate the annotations are claimed by this processor.
     */
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        annotations.forEach { annotation ->
            val elements = roundEnv.getElementsAnnotatedWith(annotation)
            elements.forEach {
                // Ensure the annotation targets a class and the class extends the expected base.
                verifyIsClass(processingEnv, it, "VaneEnchantment")
                verifyExtendsType(
                    processingEnv,
                    it,
                    "org.oddlama.vane.core.enchantments.CustomEnchantment<",
                    "VaneEnchantment",
                    "org.oddlama.vane.core.enchantments.CustomEnchantment"
                )
            }
        }

        return true
    }
}
