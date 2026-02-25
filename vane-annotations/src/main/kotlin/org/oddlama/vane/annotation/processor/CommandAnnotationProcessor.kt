package org.oddlama.vane.annotation.processor

import org.oddlama.vane.annotation.command.Name
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

private val mandatoryAnnotations: Array<Class<out Annotation>> = arrayOf(Name::class.java)

@SupportedAnnotationTypes(
    "org.oddlama.vane.annotation.command.Aliases",
    "org.oddlama.vane.annotation.command.Name",
    "org.oddlama.vane.annotation.command.VaneCommand"
)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class CommandAnnotationProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        annotations.forEach { annotation ->
            val elements = roundEnv.getElementsAnnotatedWith(annotation)
            elements.forEach { verifyIsClass(processingEnv, it, annotation.simpleName.toString()) }
            elements.forEach { verifyExtendsType(processingEnv, it, "org.oddlama.vane.core.command.Command<", annotation.simpleName.toString(), "org.oddlama.vane.core.command.Command") }

            // Verify that all mandatory annotations are present
            if (annotation.asType().toString() == "org.oddlama.vane.annotation.command.VaneCommand") {
                elements.forEach { verifyHasAnnotations(it) }
            }
        }

        return true
    }

    private fun verifyHasAnnotations(element: Element) {
        // Only check subclasses
        if (element.asType().toString().startsWith("org.oddlama.vane.core.command.Command<")) return

        mandatoryAnnotations.forEach { aCls ->
            if (element.getAnnotation(aCls) == null) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "${element.asType()}: missing @${aCls.simpleName} annotation"
                )
            }
        }
    }
}
