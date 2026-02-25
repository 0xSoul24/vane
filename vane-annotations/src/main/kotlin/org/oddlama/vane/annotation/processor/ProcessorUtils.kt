package org.oddlama.vane.annotation.processor

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.tools.Diagnostic

fun verifyIsClass(processingEnv: ProcessingEnvironment, element: Element, annotationName: String) =
    if (element.kind != ElementKind.CLASS) {
        processingEnv.messager.printMessage(
            Diagnostic.Kind.ERROR,
            "${element.asType()}: @$annotationName must be applied to a class"
        )
    } else Unit

fun verifyExtendsType(
    processingEnv: ProcessingEnvironment,
    element: Element,
    requiredSuperPrefix: String,
    annotationName: String,
    requiredFullName: String
) {
    val start = element as? TypeElement ?: run {
        processingEnv.messager.printMessage(
            Diagnostic.Kind.ERROR,
            "${element.asType()}: @$annotationName must be applied to a class"
        )
        return
    }

    var t: TypeElement? = start
    while (t != null) {
        if (t.asType().toString().startsWith(requiredSuperPrefix)) return

        val sc = t.superclass
        t = if (sc is DeclaredType) (sc.asElement() as? TypeElement) else null
    }

    processingEnv.messager.printMessage(
        Diagnostic.Kind.ERROR,
        "${start.asType()}: @$annotationName must be applied to a class inheriting from $requiredFullName"
    )
}
