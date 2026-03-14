package org.oddlama.vane.core.functional

/** Erased single-argument function functor. */
fun interface Function1<T1, R> : ErasedFunctor, GenericsFinder {
    /** Applies this function. */
    fun apply(t1: T1?): R?

    /** Invokes this function with erased arguments. */
    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: List<Any?>): Any? {
        require(args.size == 1) { "Functor needs 1 arguments but got ${args.size} arguments" }
        return apply(args[0] as T1?)
    }
}
