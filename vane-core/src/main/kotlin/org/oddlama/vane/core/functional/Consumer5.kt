package org.oddlama.vane.core.functional

fun interface Consumer5<T1, T2, T3, T4, T5> : ErasedFunctor, GenericsFinder {
    fun apply(t1: T1?, t2: T2?, t3: T3?, t4: T4?, t5: T5?)

    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: MutableList<Any?>?): Any? {
        require(args!!.size == 5) { "Functor needs 5 arguments but got ${args.size} arguments" }
        apply(args[0] as T1?, args[1] as T2?, args[2] as T3?, args[3] as T4?, args[4] as T5?)
        return null
    }
}
