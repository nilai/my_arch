package com.my.dataflow

interface SideEffect {
    fun handle(action: Action)
}