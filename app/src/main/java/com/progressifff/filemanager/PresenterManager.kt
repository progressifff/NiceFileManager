package com.progressifff.filemanager

import android.os.Bundle
import android.support.v4.util.LongSparseArray
import com.progressifff.filemanager.presenters.BasePresenter
import java.lang.Exception
import java.util.concurrent.atomic.AtomicLong

class PresenterManager private constructor() {

    private var curPresenterId: AtomicLong = AtomicLong()

    private var presenters = LongSparseArray<BasePresenter<*, *>>()

    private object Holder { val INSTANCE = PresenterManager() }

    @Suppress("UNCHECKED_CAST")
    fun <P : BasePresenter<*, *>> restorePresenter(savedInstanceState: Bundle, presenterKey: String = PRESENTER_ID): P{
        val presenterId = savedInstanceState.getLong(presenterKey)
        val presenter = presenters.get(presenterId)
        if(presenter != null) {
            presenters.remove(presenterId)
            return presenter as P
        }
        throw Exception("Failed to restore presenter")
    }

    fun savePresenter(presenter: BasePresenter<*, *>, outState: Bundle, presenterKey: String = PRESENTER_ID){
        val presenterId = curPresenterId.incrementAndGet()
        presenters.put(presenterId, presenter)
        outState.putLong(presenterKey, presenterId)
    }

    companion object {
        const val PRESENTER_ID: String = "presenter_id"

        val instance: PresenterManager by lazy { Holder.INSTANCE }
    }
}