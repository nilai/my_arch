package com.my.dataflow

interface Reducer<S> {
    fun reduce(oldState: S, action: Action): S
}

typealias ReducerType<S> = (state: S, action: Action) -> S