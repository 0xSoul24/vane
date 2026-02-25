package org.oddlama.vane.annotation.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedAnnotationTypes("org.oddlama.vane.annotation.enchantment.VaneEnchantment")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class VaneEnchantmentProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        annotations.forEach { annotation ->
            val elements = roundEnv.getElementsAnnotatedWith(annotation)
            elements.forEach { verifyIsClass(processingEnv, it, "VaneEnchantment") }
            elements.forEach { verifyExtendsType(processingEnv, it, "org.oddlama.vane.core.enchantments.CustomEnchantment<", "VaneEnchantment", "org.oddlama.vane.core.enchantments.CustomEnchantment") }
        }

        return true
    }
}
