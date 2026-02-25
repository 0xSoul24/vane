package org.oddlama.vane.core.functional

fun interface Function2<T1, T2, R> : ErasedFunctor, GenericsFinder {
    fun apply(t1: T1?, t2: T2?): R?

    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: MutableList<Any?>?): Any? {
        require(args!!.size == 2) { "Functor needs 2 arguments but got ${args.size} arguments" }
        return apply(args[0] as T1?, args[1] as T2?)
    }
}
