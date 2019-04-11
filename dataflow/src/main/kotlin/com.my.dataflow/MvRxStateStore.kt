package com.my.dataflow

import io.reactivex.Observable
import io.reactivex.disposables.Disposable

interface MvRxStateStore<S : Any> : Disposable {
    val state: S
    fun get(block: (S) -> Unit)
    fun set(stateReducer: S.() -> S)
    val observable: Observable<S>
}
