package com.my.dataflow

class AppReducer : Reducer<AppState> {
    override fun reduce(oldState: AppState, action: Action): AppState {
        return when(action) {
            is AppActions.LoginAction -> oldState
            else -> oldState
        }
    }
}