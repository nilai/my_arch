package com.my.dataflow

typealias DispatchFunction = (Action) -> Unit

interface Middleware<S: State> {
    fun apply(storeDispatch: DispatchFunction) : (DispatchFunction) -> DispatchFunction
}