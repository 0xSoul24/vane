package org.oddlama.vane.annotation.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@SupportedAnnotationTypes("org.oddlama.vane.annotation.enchantment.VaneEnchantment")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class VaneEnchantmentProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement?>, roundEnv: RoundEnvironment): Boolean {
        for (annotation in annotations) {
            roundEnv.getElementsAnnotatedWith(annotation).forEach { element: Element -> verifyIsClass(processingEnv, element, "VaneEnchantment") }
            roundEnv.getElementsAnnotatedWith(annotation)
                .forEach { element: Element? -> verifyExtendsType(processingEnv, element, "org.oddlama.vane.core.enchantments.CustomEnchantment<", "VaneEnchantment", "org.oddlama.vane.core.enchantments.CustomEnchantment") }
        }

        return true
    }
}
