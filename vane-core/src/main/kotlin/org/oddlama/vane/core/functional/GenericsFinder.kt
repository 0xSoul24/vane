package org.oddlama.vane.core.functional

import java.io.Serializable
import java.lang.invoke.SerializedLambda
import java.lang.reflect.Method
import java.util.*
import java.util.function.Supplier

interface GenericsFinder : Serializable {
    fun serialized(): SerializedLambda {
        try {
            val replaceMethod = javaClass.getDeclaredMethod("writeReplace")
            replaceMethod.setAccessible(true)
            return replaceMethod.invoke(this) as SerializedLambda
        } catch (e: Exception) {
            throw java.lang.RuntimeException(e)
        }
    }

    val containingClass: Class<*>
        get() {
            try {
                val className = serialized().implClass.replace("/".toRegex(), ".")
                return Class.forName(className)
            } catch (e: Exception) {
                throw java.lang.RuntimeException(e)
            }
        }

    fun method(): Method {
        val lambda = serialized()
        val containingClass = this.containingClass
        return Arrays.stream(containingClass.getDeclaredMethods())
            .filter { method: Method -> method.name == lambda.implMethodName }
            .findFirst()
            .orElseThrow(Supplier { RuntimeException() })
    }
}
