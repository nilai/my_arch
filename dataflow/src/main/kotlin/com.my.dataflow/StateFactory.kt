package com.my.dataflow

import android.app.Activity
import android.app.Application
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

object StateFactory {
    fun <S : MvRxState> createInitialState(
            stateClass: Class<out S>,
            stateContext: StateContext,
            stateRestorer: (S) -> S
    ): S {
        return stateRestorer(createStateFromConstructor(stateClass, stateContext.args))
    }

    internal fun <S : MvRxState> createStateFromConstructor(stateClass: Class<S>, args: Any?): S {
        val argsConstructor = args?.let { arg ->
            val argType = arg::class.java

            stateClass.constructors.firstOrNull { constructor ->
                constructor.parameterTypes.size == 1 && isAssignableTo(argType, constructor.parameterTypes[0])
            }
        }

        @Suppress("UNCHECKED_CAST")
        return argsConstructor?.newInstance(args) as? S
                ?: try {
                    stateClass.newInstance()
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    null
                }
                ?: throw IllegalStateException(
                        "Attempt to create the MvRx state class ${stateClass.simpleName} has failed. One of the following must be true:" +
                                "\n 1) The state class has default values for every constructor property." +
                                "\n 2) The state class has a secondary constructor for ${args?.javaClass?.simpleName
                                        ?: "a fragment argument"}." +
                                "\n 3) The ViewModel using the state must have a companion object implementing MvRxFactory with an initialState function " +
                                "that does not return null. "
                )
    }
}

sealed class StateContext {
    abstract val args: Any?

    fun <A> args(): A = args as A
}

class ApplicationStateContext(
        val application: Application,
        override val args: Any?
) : StateContext() {
    fun <A : Application> application(): A = application as A
}

class ActivityStateContext(
        val activity: FragmentActivity,
        override val args: Any?
) : StateContext() {
    fun <A : Activity> activity(): A = activity as A
}

class FragmentStateContext(
        val activity: FragmentActivity,
        val fragment: Fragment,
        override val args: Any?
) : StateContext() {
    fun <F : Fragment> fragment(): F = fragment as F
}