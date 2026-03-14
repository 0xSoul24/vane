package org.oddlama.vane.core.functional

/** Erased two-argument function functor. */
fun interface Function2<T1, T2, R> : ErasedFunctor, GenericsFinder {
    /** Applies this function. */
    fun apply(t1: T1?, t2: T2?): R?

    /** Invokes this function with erased arguments. */
    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: List<Any?>): Any? {
        require(args.size == 2) { "Functor needs 2 arguments but got ${args.size} arguments" }
        return apply(args[0] as T1?, args[1] as T2?)
    }
}
