package org.oddlama.vane.core.functional

interface ErasedFunctor {
    fun invoke(args: List<Any?>): Any?
}
