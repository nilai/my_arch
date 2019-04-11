package com.my.dataflow

import io.reactivex.subjects.BehaviorSubject
import java.util.*

class DefaultStateStore private constructor() : StateStore {
//    private val subject: BehaviorSubject<AppState> = BehaviorSubject.createDefault(initialState)

//    override val state: AppState
//        get() = subject.value!!

//    override val observable: Observable<AppState>
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    companion object {
//        fun getStore(): StateStore {
//            TODO()
//
//        }

        private var INST: DefaultStateStore? = null

        private val lock = Any()

        fun getStore(): DefaultStateStore {
            synchronized(lock) {
                if (INST == null) {
                    INST = DefaultStateStore()
                }
                return INST!!
            }
        }
    }

    override fun <S> get(block: (S) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }

    override fun <S> set(stateReducer: S.() -> S) {
    }

    override fun dispose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isDisposed(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private class Jobs<S> {

        private val getStateQueue = LinkedList<(state: S) -> Unit>()
        private var setStateQueue = LinkedList<S.() -> S>()

        @Synchronized
        fun enqueueGetStateBlock(block: (state: S) -> Unit) {
            getStateQueue.add(block)
        }

        @Synchronized
        fun enqueueSetStateBlock(block: S.() -> S) {
            setStateQueue.add(block)
        }

        @Synchronized
        fun dequeueGetStateBlock(): ((state: S) -> Unit)? {
            return getStateQueue.poll()
        }

        @Synchronized
        fun dequeueSetStateBlock(): (S.() -> S)? {
            return setStateQueue.poll()
        }
    }
}