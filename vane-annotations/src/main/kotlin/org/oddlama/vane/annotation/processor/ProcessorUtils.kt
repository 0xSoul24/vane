package org.oddlama.vane.annotation.processor

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.tools.Diagnostic

// Verifica que el elemento sea una clase; si no, imprime un error con el nombre de la anotación
fun verifyIsClass(processingEnv: ProcessingEnvironment, element: Element, annotationName: String) {
    if (element.kind != ElementKind.CLASS) {
        processingEnv
            .messager
            .printMessage(
                Diagnostic.Kind.ERROR,
                element.asType().toString() + ": @" + annotationName + " must be applied to a class"
            )
    }
}

// Verifica que el elemento herede de una clase cuyo tipo comienza por `requiredSuperPrefix`.
// Si no, imprime un error mencionando `requiredFullName` como requisito legible.
fun verifyExtendsType(
    processingEnv: ProcessingEnvironment,
    element: Element?,
    requiredSuperPrefix: String,
    annotationName: String,
    requiredFullName: String
) {
    var t = element as TypeElement
    while (true) {
        if (t.asType().toString().startsWith(requiredSuperPrefix)) {
            return
        }
        if (t.superclass is DeclaredType) {
            t = (t.superclass as DeclaredType).asElement() as TypeElement
        } else {
            break
        }
    }

    processingEnv
        .messager
        .printMessage(
            Diagnostic.Kind.ERROR,
            t.asType().toString() +
                    ": @" + annotationName +
                    " must be applied to a class inheriting from " + requiredFullName
        )
}
