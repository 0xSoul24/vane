package org.oddlama.vane.core.functional

fun interface Consumer4<T1, T2, T3, T4> : ErasedFunctor, GenericsFinder {
    fun apply(t1: T1?, t2: T2?, t3: T3?, t4: T4?)

    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: MutableList<Any?>?): Any? {
        require(args!!.size == 4) { "Functor needs 4 arguments but got ${args.size} arguments" }
        apply(args[0] as T1?, args[1] as T2?, args[2] as T3?, args[3] as T4?)
        return null
    }
}
