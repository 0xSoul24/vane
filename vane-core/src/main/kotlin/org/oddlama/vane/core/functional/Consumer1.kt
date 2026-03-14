package org.oddlama.vane.core.functional

/** Erased single-argument consumer functor. */
fun interface Consumer1<T1> : ErasedFunctor, GenericsFinder {
    /** Applies this consumer. */
    fun apply(t1: T1?)

    /** Invokes this consumer with erased arguments. */
    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: List<Any?>): Any? {
        require(args.size == 1) { "Functor needs 1 arguments but got ${args.size} arguments" }
        apply(args[0] as T1?)
        return null
    }
}
