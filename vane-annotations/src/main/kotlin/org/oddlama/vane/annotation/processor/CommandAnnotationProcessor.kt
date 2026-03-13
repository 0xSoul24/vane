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

/** Required annotations that must be present on classes using `@VaneCommand`. */
private val mandatoryAnnotations = listOf(Name::class.java)

/**
 * Validates command-related annotations such as `@VaneCommand`, `@Name` and `@Aliases`.
 *
 * Ensures annotated classes are valid command implementations and warns/errors when
 * required annotations are missing.
 */
@SupportedAnnotationTypes(
    "org.oddlama.vane.annotation.command.Aliases",
    "org.oddlama.vane.annotation.command.Name",
    "org.oddlama.vane.annotation.command.VaneCommand"
)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class CommandAnnotationProcessor : AbstractProcessor() {
    /**
     * Processes command-related annotations and validates their usage.
     *
     * Checks that annotated elements are classes and extend the expected
     * command base type. For `@VaneCommand` annotated classes it also verifies
     * that mandatory annotations (like `@Name`) are present.
     *
     * @param annotations Annotation types to process.
     * @param roundEnv Information about the current processing round.
     * @return true to indicate that the annotations have been claimed.
     */
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        annotations.forEach { annotation ->
            val elements = roundEnv.getElementsAnnotatedWith(annotation)
            elements.forEach {
                verifyIsClass(processingEnv, it, annotation.simpleName.toString())
                verifyExtendsType(
                    processingEnv,
                    it,
                    "org.oddlama.vane.core.command.Command<",
                    annotation.simpleName.toString(),
                    "org.oddlama.vane.core.command.Command"
                )
            }

            // Verify that all mandatory annotations are present
            if (annotation.asType().toString() == "org.oddlama.vane.annotation.command.VaneCommand") {
                elements.forEach { verifyHasAnnotations(it) }
            }
        }

        return true
    }

    /**
     * Ensures that required command annotations (like `@Name`) are present on the class.
     * Skips checks for classes that are already subclasses of the framework's generic Command.
     *
     * @param element The element (class) to validate for required annotations.
     */
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
