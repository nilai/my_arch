package com.my.dataflow

import io.reactivex.disposables.Disposable

interface StateStore : Disposable {
//    val state: S
    fun <S> get(block: (S) -> Unit)
    fun <S> set(stateReducer: S.() -> S)
//    val observable: Observable<S>
}