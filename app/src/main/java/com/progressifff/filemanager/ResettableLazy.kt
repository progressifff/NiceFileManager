package com.progressifff.filemanager

import java.util.*
import kotlin.reflect.KProperty

open class ResettableLazyManager{
    interface Resettable{
        fun reset()
    }
    private val managedDelegates = LinkedList<Resettable>()

    fun register(managed: Resettable){
        synchronized(managedDelegates) {
            managedDelegates.add(managed)
        }
    }

    fun reset(){
        synchronized(managedDelegates){
            managedDelegates.forEach {
                it.reset()
            }
            managedDelegates.clear()
        }
    }
}

class ResettableLazy<T>(private val manager: ResettableLazyManager, val init: ()->T) : ResettableLazyManager.Resettable{
    @Volatile var lazyHolder = createLazyHolder()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T{
        return lazyHolder.value
    }

    override fun reset(){
        lazyHolder = createLazyHolder()
    }

    private fun createLazyHolder(): Lazy<T>{
        return lazy{
            manager.register(this)
            init()
        }
    }
}

fun <T> resettableLazy(manager: ResettableLazyManager, init: () -> T): ResettableLazy<T>{
    return ResettableLazy(manager, init)
}