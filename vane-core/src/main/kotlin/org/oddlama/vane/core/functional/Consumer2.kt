package org.oddlama.vane.core.functional

fun interface Consumer2<T1, T2> : ErasedFunctor, GenericsFinder {
    fun apply(t1: T1?, t2: T2?)

    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: MutableList<Any?>?): Any? {
        require(args!!.size == 2) { "Functor needs 2 arguments but got ${args.size} arguments" }
        apply(args[0] as T1?, args[1] as T2?)
        return null
    }
}
