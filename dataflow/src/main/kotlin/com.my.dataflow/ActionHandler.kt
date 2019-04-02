package com.my.dataflow

interface ActionHandler<A : Action> {
    fun handle(action: A, actionDispatch: DispatchFunction)
}

//typealias ActionDispatch = (Action) -> Unit