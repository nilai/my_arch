package com.my.dataflow

class StateProvider private constructor(private val map: HashMap<String, State>,
                    var factory: Factory) {
    companion object {
        private var INST: StateProvider? = null

        private val lock = Any()

        fun getInstance(): StateProvider {
            synchronized(lock) {
                if (INST == null) {
                    INST = StateProvider(map = HashMap(), factory = NewInstanceFactory())
                }
                return INST!!
            }
        }
    }

    fun <T : State> get(modelClass: Class<T>): State {
        val canonicalName = modelClass.canonicalName
                ?: throw IllegalArgumentException("Local and anonymous classes can not be ViewModels")
        var state = get(canonicalName)
        if (state != null && modelClass.isInstance(state)) {
            return state
        } else {

        }

        state = factory.create(modelClass)
        put(canonicalName, state)

        return state
    }

    fun clear() {
//        map.forEach { it.value.onCleared() }
        map.clear()
    }

    private fun get(key: String): State? {
        return map[key]
    }

    private fun put(key: String, state: State) {
//        map[key]?.onCleared()
        map[key] = state
    }
}

interface Factory {
    fun <T : State> create(modelClass: Class<T>): State
}

class NewInstanceFactory : Factory {
    override fun <T : State> create(modelClass: Class<T>): State {
        try {
            return modelClass.newInstance()
        } catch (e: InstantiationException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        }
    }

}