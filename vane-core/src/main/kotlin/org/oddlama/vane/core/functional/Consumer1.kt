package org.oddlama.vane.core.functional

fun interface Consumer1<T1> : ErasedFunctor, GenericsFinder {
    fun apply(t1: T1?)

    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: MutableList<Any?>?): Any? {
        require(args!!.size == 1) { "Functor needs 1 arguments but got ${args.size} arguments" }
        apply(args[0] as T1?)
        return null
    }
}
