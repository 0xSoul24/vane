package org.oddlama.vane.annotation.processor

import org.oddlama.vane.annotation.command.Name
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@SupportedAnnotationTypes(
    "org.oddlama.vane.annotation.command.Aliases",
    "org.oddlama.vane.annotation.command.Name",
    "org.oddlama.vane.annotation.command.VaneCommand"
)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class CommandAnnotationProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (annotation in annotations) {
            roundEnv.getElementsAnnotatedWith(annotation).forEach { e: Element? -> verifyIsClass(annotation, e!!) }
            roundEnv.getElementsAnnotatedWith(annotation).forEach { e: Element? -> verifyExtendsCommand(annotation, e) }

            // Verify that all mandatory annotations are present
            if (annotation.asType().toString() == "org.oddlama.vane.annotation.command.VaneCommand") {
                roundEnv.getElementsAnnotatedWith(annotation)
                    .forEach { element: Element -> this.verifyHasAnnotations(element) }
            }
        }

        return true
    }

    private fun verifyHasAnnotations(element: Element) {
        // Only check subclasses
        if (element.asType().toString().startsWith("org.oddlama.vane.core.command.Command<")) {
            return
        }

        for (aCls in mandatoryAnnotations) {
            if (element.getAnnotation(aCls) == null) {
                processingEnv
                    .messager
                    .printMessage(
                        Diagnostic.Kind.ERROR,
                        element.asType().toString() + ": missing @" + aCls.simpleName + " annotation"
                    )
            }
        }
    }

    private fun verifyIsClass(annotation: TypeElement, element: Element) {
        if (element.kind != ElementKind.CLASS) {
            processingEnv
                .messager
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    element.asType().toString() + ": @" + annotation.simpleName + " must be applied to a class"
                )
        }
    }

    private fun verifyExtendsCommand(annotation: TypeElement, element: Element?) {
        val t = element as TypeElement
        if (t.toString() != "org.oddlama.vane.core.command.Command" &&
            !t.superclass.toString().startsWith("org.oddlama.vane.core.command.Command<")
        ) {
            processingEnv
                .messager
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    element.asType().toString() +
                            ": @" +
                            annotation.simpleName +
                            " must be applied to a class inheriting from org.oddlama.vane.core.command.Command, but it inherits from " +
                            t.superclass.toString()
                )
        }
    }

    companion object {
        private val mandatoryAnnotations: Array<Class<out Annotation>> = arrayOf(Name::class.java)
    }
}
