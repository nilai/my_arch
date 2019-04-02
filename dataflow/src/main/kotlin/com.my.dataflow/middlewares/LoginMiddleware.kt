package com.my.dataflow.middlewares

import android.util.Log
import com.my.dataflow.*

class LoginMiddleware : Middleware<State> {
    override fun apply(storeDispatch: DispatchFunction): (DispatchFunction) -> DispatchFunction {
        return { next ->
            { action ->
                if (action !is Event) {
                    Log.d(Constants.DATA_FLOW_TAG, "Login action: ${action.javaClass}")
                    when (action) {
                        is AppActions.LoginAction -> next(AppActions.LoginingAciton)
                        else -> next(action)
                    }
                } else {
                    next(action)
                }
            }
        }
    }
}