package com.my.dataflow

import androidx.lifecycle.LifecycleOwner
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

abstract class BaseMvRxStatePresenter<S : MvRxState>(
        initialState: S
) : StatePresenter {
    private val stateStore: MvRxStateStore<S> = RealMvRxStateStore(initialState)
    private val disposables = CompositeDisposable()
    private val lastDeliveredStates = ConcurrentHashMap<String, Any>()
    private val activeSubscriptions = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    init {
        // Kotlin reflection has a large overhead the first time you run it
        // but then is pretty fast on subsequent times. Running these methods now will
        // initialize kotlin reflect and warm the cache so that when persistState() gets
        // called synchronously in onSaveInstanceState() on the main thread, it will be
        // much faster.
        // This improved performance 10-100x for a state with 100 @PersistStae properties.
        Completable.fromCallable {
            initialState::class.primaryConstructor?.parameters?.forEach { it.annotations }
            initialState::class.declaredMemberProperties.asSequence()
                    .filter { it.isAccessible }
                    .forEach { prop ->
                        @Suppress("UNCHECKED_CAST")
                        (prop as? KProperty1<S, Any?>)?.get(initialState)
                    }
        }.subscribeOn(Schedulers.computation()).subscribe()

//        if (this.debugMode) {
//            mutableStateChecker = MutableStateChecker(initialState)
//
//            Completable.fromCallable { validateState(initialState) }
//                    .subscribeOn(Schedulers.computation()).subscribe()
//        }
    }

    internal val state: S
        get() = stateStore.state

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

    fun subscribe(subscriber: (S) -> Unit) =
            stateStore.observable.subscribeLifecycle(null, RedeliverOnStart, subscriber)

    fun subscribe(owner: LifecycleOwner, subscriber: (S) -> Unit) =
            stateStore.observable.subscribeLifecycle(owner, RedeliverOnStart, subscriber)

    fun subscribe(owner: LifecycleOwner, deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) =
            stateStore.observable.subscribeLifecycle(owner, deliveryMode, subscriber)

    fun <T> Single<T>.execute(
            stateReducer: S.(Async<T>) -> S
    ) = toObservable().execute({ it }, null, stateReducer)

    fun <T> Observable<T>.execute(
            stateReducer: S.(Async<T>) -> S
    ) = execute({ it }, null, stateReducer)

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

    fun <T> asyncSubscribe(
            asyncProp: KProperty1<S, Async<T>>,
            onFail: ((Throwable) -> Unit)? = null,
            onSuccess: ((T) -> Unit)? = null
    ) = asyncSubscribeInternal(null, asyncProp, RedeliverOnStart, onFail, onSuccess)

    private fun <T> asyncSubscribeInternal(
            owner: LifecycleOwner?,
            asyncProp: KProperty1<S, Async<T>>,
            deliveryMode: DeliveryMode,
            onFail: ((Throwable) -> Unit)? = null,
            onSuccess: ((T) -> Unit)? = null
    ) = selectSubscribeInternal(owner, asyncProp, deliveryMode.appendPropertiesToId(asyncProp)) { asyncValue ->
        if (onSuccess != null && asyncValue is Success) {
            onSuccess(asyncValue())
        } else if (onFail != null && asyncValue is Fail) {
            onFail(asyncValue.error)
        }
    }

    private fun <A> selectSubscribeInternal(
            owner: LifecycleOwner?,
            prop1: KProperty1<S, A>,
            deliveryMode: DeliveryMode,
            subscriber: (A) -> Unit
    ) = stateStore.observable
            .map { MvRxTuple1(prop1.get(it)) }
            .distinctUntilChanged()
            .subscribeLifecycle(owner, deliveryMode.appendPropertiesToId(prop1)) { (a) -> subscriber(a) }

    private fun <T : Any> Observable<T>.subscribeLifecycle(
            lifecycleOwner: LifecycleOwner? = null,
            deliveryMode: DeliveryMode,
            subscriber: (T) -> Unit
    ): Disposable {
        if (lifecycleOwner == null) {
            return observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subscriber)
                    .disposeOnClear()
        }

        val lifecycleAwareObserver = MvRxLifecycleAwareObserver(
                lifecycleOwner,
                deliveryMode = deliveryMode,
                lastDeliveredValue = if (deliveryMode is UniqueOnly) {
                    if (activeSubscriptions.contains(deliveryMode.subscriptionId)) {
                        throw IllegalStateException("Subscribing with a duplicate subscription id: ${deliveryMode.subscriptionId}. " +
                                "If you have multiple uniqueOnly subscriptions in a MvRx view that listen to the same properties " +
                                "you must use a custom subscription id. If you are using a custom MvRxView, make sure you are using the proper" +
                                "lifecycle owner. See BaseMvRxFragment for an example.")
                    }
                    activeSubscriptions.add(deliveryMode.subscriptionId)
                    lastDeliveredStates[deliveryMode.subscriptionId] as? T
                } else {
                    null
                },
                onNext = Consumer { value ->
                    if (deliveryMode is UniqueOnly) {
                        lastDeliveredStates[deliveryMode.subscriptionId] = value
                    }
                    subscriber(value)
                },
                onDispose = {
                    if (deliveryMode is UniqueOnly) {
                        activeSubscriptions.remove(deliveryMode.subscriptionId)
                    }
                }
        )
        return observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(lifecycleAwareObserver)
                .disposeOnClear()
    }
}

/**
 * Defines what updates a subscription should receive.
 * See: [RedeliverOnStart], [UniqueOnly].
 */
sealed class DeliveryMode {

    internal fun appendPropertiesToId(vararg properties: KProperty1<*, *>) : DeliveryMode {
        return when (this) {
            is RedeliverOnStart -> RedeliverOnStart
            is UniqueOnly -> UniqueOnly(subscriptionId + "_" + properties.joinToString(",") { it.name })
        }
    }
}

/**
 * The subscription will receive the most recent state update when transitioning from locked to unlocked states (stopped -> started),
 * even if the state has not changed while locked.
 *
 * Likewise, when a MvRxView resubscribes after a configuration change the most recent update will always be emitted.
 */
object RedeliverOnStart : DeliveryMode()

/**
 * The subscription will receive the most recent state update when transitioning from locked to unlocked states (stopped -> started),
 * only if the state has changed while locked.
 *
 * Likewise, when a MvRxView resubscribes after a configuration change the most recent update will only be emitted
 * if the state has changed while locked.
 *
 * @param subscriptionId A uniqueIdentifier for this subscription. It is an error for two unique only subscriptions to
 * have the same id.
 */
class UniqueOnly(val subscriptionId: String) : DeliveryMode()