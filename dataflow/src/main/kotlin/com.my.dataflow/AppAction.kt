package com.my.dataflow

sealed class AppActions : Action {

    object LoginAction : AppActions()
    object LoginingAciton : AppActions()

}
