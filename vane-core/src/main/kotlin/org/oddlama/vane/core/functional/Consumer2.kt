package org.oddlama.vane.core.functional

/** Erased two-argument consumer functor. */
fun interface Consumer2<T1, T2> : ErasedFunctor, GenericsFinder {
    /** Applies this consumer. */
    fun apply(t1: T1?, t2: T2?)

    /** Invokes this consumer with erased arguments. */
    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: List<Any?>): Any? {
        require(args.size == 2) { "Functor needs 2 arguments but got ${args.size} arguments" }
        apply(args[0] as T1?, args[1] as T2?)
        return null
    }
}
