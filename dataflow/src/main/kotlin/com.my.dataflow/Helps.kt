package com.my.dataflow


class CombinedReducer : Reducer<State> {
    val reducers = mutableListOf<Reducer<State>>()

    override fun reduce(oldState: State, action: Action): State {
        reducers.forEach {
            val newState = it.reduce(oldState, action)
            if (newState != oldState) {
                return newState
            }
        }

        return oldState
    }

}

//data class ReducerWithAction<S : State>(val action: Action, val reducer: Reducer<S>)

fun combineReducers(reducers: List<Reducer<State>>): Reducer<State> {
    val combinedReducer = CombinedReducer()

    combinedReducer.reducers.addAll(reducers)

    return combinedReducer
}