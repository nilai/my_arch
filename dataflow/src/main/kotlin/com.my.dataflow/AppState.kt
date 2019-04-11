package com.my.dataflow

data class AppState(
        var networkState: Int,
        var isLogin: Boolean = false
) : State {
}
