package com.my.dataflow

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import io.reactivex.subjects.PublishSubject

class StoreProvider {
    companion object {
        @Volatile
        private var INST: Store<State>? = null

        fun  get(initialState: State, rootReducer: Reducer<State>, middlewares: List<Middleware<State>>): Store<State> =
                INST ?: synchronized(this) {
                    INST ?: build(initialState, rootReducer, middlewares).also { INST = it }
                }

        private fun build(initialState: State, rootReducer: Reducer<State>, middlewares: List<Middleware<State>>): Store<State> {
            val store = Store(currentstate = initialState, reducer = rootReducer, middlewares = middlewares)
            store.initialize()

            return store
        }
    }
}

class Store<S : State>(
        private val dispatchThread: HandlerThread = HandlerThread("Dispatch-Thread"),
        private var reducer: Reducer<S>,
        private var dispatchHandler: Handler? = null,
        private val subcriber: PublishSubject<StateResult> = PublishSubject.create(),
        private var currentstate: S,
        private val middlewares: List<Middleware<State>>,
        private val sideEffects: MutableList<SideEffect> = mutableListOf()) {

    private val dispatchFunction: DispatchFunction = middlewares
            .reversed()
            .fold({ action: Action -> _dispatchReducer(action) }, { dispatchFunction, middleware ->
                val dispatch = { action: Action -> dispatchFunction(action) }
                middleware.apply(dispatch)(dispatchFunction)
            })

    fun initialize() {
        dispatchThread.start()
        dispatchHandler = Handler(dispatchThread.looper, Handler.Callback { msg ->
            if (msg.what == 1) {
                val action = msg.obj as Action
                Log.d(Constants.DATA_FLOW_TAG, "Dispatch: ${action.javaClass}")
                dispatchFunction.invoke(action)
                return@Callback true
            }
            false
        })
    }


    fun depose() {
        dispatchThread.quit()
    }

    fun getState(): S {
        return currentstate;
    }

    fun addSideEffect(sideEffect: SideEffect) {
        sideEffects.add(sideEffect)
    }

    fun dispatch(action: Action) {
        dispatchHandler!!.sendMessage(dispatchHandler!!.obtainMessage(1, action))
    }

    fun _dispatchReducer(action: Action) {
        val newState = reducer.reduce(currentstate, action)
        if (currentstate != newState && !currentstate.equals(newState)) {
//            Log.d(Constants.DATA_FLOW_TAG, "_dispatchReducer new state: ${newState.javaClass}")
            currentstate = newState
            _notifyStateChanged(action, newState)
        }

        sideEffects.forEach {
            it.handle(action)
        }
    }

    fun _notifyStateChanged(action: Action, newState: S) {
        subcriber.onNext(StateResult(action, newState))
    }

//    fun subcribe(observer: Observer<StateResult>): Subscription {
//        return subcriber.subscribe(observer)
//    }

}

typealias ActionCreator<State, Store> = (state: State, store: Store) -> Action?

data class StateResult(val action: Action, val state: State)
