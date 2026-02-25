package org.oddlama.vane.core.functional

fun interface Function1<T1, R> : ErasedFunctor, GenericsFinder {
    fun apply(t1: T1?): R?

    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: MutableList<Any?>?): Any? {
        require(args!!.size == 1) { "Functor needs 1 arguments but got ${args.size} arguments" }
        return apply(args[0] as T1?)
    }
}
