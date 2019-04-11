package com.my.dataflow

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseStatePresenter<S : State>(
        initialState: S
) : StatePresenter {
    private val stateStore: StateStore = DefaultStateStore.getStore()

    private val disposables = CompositeDisposable()


//    init {
//        stateStore = DefaultStateStore.getStore(initialState)
//    }

    protected fun withState(block: (state: S) -> Unit) {
        stateStore.get(block)
    }

    protected fun setState(reducer: S.() -> S) {
        stateStore.set(reducer)
    }

    protected fun Disposable.disposeOnClear(): Disposable {
        disposables.add(this)
        return this
    }

    override fun onCleared() {
        disposables.dispose()
    }

    fun <T> Single<T>.execute(
            stateReducer: S.(Async<T>) -> S
    ) = toObservable().execute({ it }, null, stateReducer)

    fun <T, V> Observable<T>.execute(
            mapper: (T) -> V,
            successMetaData: ((T) -> Any)? = null,
            stateReducer: S.(Async<V>) -> S
    ): Disposable {
        setState { stateReducer(Loading()) }

        return map { value ->
            val success = Success(mapper(value))
            success.metadata = successMetaData?.invoke(value)
            success as Async<V>
        }
                .onErrorReturn { Fail(it) }
                .subscribe { asyncData -> setState { stateReducer(asyncData) } }
                .disposeOnClear()
    }
}