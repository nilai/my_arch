package com.my.dataflow

import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

inline fun<T, reified P: BaseMvRxStatePresenter<S>, reified S : MvRxState>T.fragmentPresenter(
        presenter: KClass<P> = P::class
) where T : Fragment = lifecycleAwareLazy(this) {

}