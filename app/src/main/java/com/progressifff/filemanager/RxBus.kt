package com.progressifff.filemanager

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.ReplaySubject

object RxBus {

    private const val REPLAY_SUBJECT_MAX_SIZE = 5

    private val publisher = ReplaySubject.createWithSize<Any>(REPLAY_SUBJECT_MAX_SIZE)

    private val stub = Any()

    fun publish(event: Any){
        publisher.onNext(event)
    }

    fun<T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType).observeOn(AndroidSchedulers.mainThread())

    fun clearHistory(){
        for(i in 0..REPLAY_SUBJECT_MAX_SIZE + 1){
            publisher.onNext(stub)
        }
    }
}