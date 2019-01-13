package com.progressifff.filemanager

import android.support.annotation.CallSuper
import io.reactivex.annotations.NonNull
import java.lang.ref.WeakReference

abstract class BasePresenter<M, V>  {
    protected abstract var model: M
    private var viewRef = WeakReference<V>(null)
    protected val view: V? get() {return viewRef.get()}

    protected open fun updateView() {}

    @CallSuper
    open fun bindView(@NonNull v: V){
        viewRef = WeakReference(v)
    }

    @CallSuper
    open fun unbindView(){
        viewRef.clear()
    }
}