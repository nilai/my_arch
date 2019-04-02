package com.my.dataflow.middlewares

import android.util.Log
import com.my.dataflow.*

class LoggingMiddleware : Middleware<State> {
    override fun apply(storeDispatch: DispatchFunction): (DispatchFunction) -> DispatchFunction {
        return { next ->
            { action ->
                if (action !is Event) {
                    Log.d(Constants.DATA_FLOW_TAG, "Logging Middleware action: ${action.javaClass}")
                }
                next(action)
            }
        }
    }
}