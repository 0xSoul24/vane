package org.oddlama.vane.core.functional

import java.io.Serializable
import java.lang.invoke.SerializedLambda
import java.lang.reflect.Method

interface GenericsFinder : Serializable {
    fun serialized(): SerializedLambda =
        runCatching {
            javaClass.getDeclaredMethod("writeReplace")
                .also { it.isAccessible = true }
                .invoke(this) as SerializedLambda
        }.getOrElse { throw RuntimeException(it) }

    val containingClass: Class<*>
        get() = runCatching {
            Class.forName(serialized().implClass.replace("/", "."))
        }.getOrElse { throw RuntimeException(it) }

    fun method(): Method {
        val lambda = serialized()
        return containingClass.declaredMethods
            .firstOrNull { it.name == lambda.implMethodName }
            ?: throw RuntimeException("Method '${lambda.implMethodName}' not found")
    }
}
