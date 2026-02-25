package org.oddlama.vane.core.functional

fun interface Function3<T1, T2, T3, R> : ErasedFunctor, GenericsFinder {
    fun apply(t1: T1?, t2: T2?, t3: T3?): R?

    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: MutableList<Any?>?): Any? {
        require(args!!.size == 3) { "Functor needs 3 arguments but got ${args.size} arguments" }
        return apply(args[0] as T1?, args[1] as T2?, args[2] as T3?)
    }
}
