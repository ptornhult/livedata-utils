package se.codeunlimited.livedata.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

/**
 * Combine 2 LiveData into a Pair, emitting only when both sources are non-null
 */
fun <A, B> combine(a: LiveData<A>, b: LiveData<B>): LiveData<Pair<A, B>> {
    return MediatorLiveData<Pair<A, B>>().apply {
        fun combine() {
            val aValue = a.value
            val bValue = b.value
            if (aValue != null && bValue != null) {
                postValue(Pair(aValue, bValue))
            }
        }

        addSource(a) { combine() }
        addSource(b) { combine() }

        combine()
    }
}

/**
 * Combine 3 LiveData into a Triple, emitting only when all three sources are non-null
 */
fun <A, B, C> combine(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>): LiveData<Triple<A, B, C>> {
    return MediatorLiveData<Triple<A, B, C>>().apply {
        fun combine() {
            val aValue = a.value
            val bValue = b.value
            val cValue = c.value
            if (aValue != null && bValue != null && cValue != null) {
                postValue(Triple(aValue, bValue, cValue))
            }
        }

        addSource(a) { combine() }
        addSource(b) { combine() }
        addSource(c) { combine() }

        combine()
    }
}

/** Extension on LiveData to combine it with another LiveData. See #combine **/
fun <A, B> LiveData<A>.combineWith(other: LiveData<B>): LiveData<Pair<A, B>> =
    combine(this, other)

/** Extension on LiveData to combine it with another LiveData. See #combine **/
fun <A, B, C> LiveData<A>.combineWith(other: LiveData<B>, secondOther: LiveData<C>):
        LiveData<Triple<A, B, C>> =
    combine(this, other, secondOther)

/**
 * Similar to Transformations.distinctUntilChanged but you define the predicate in isDistinct
 * @param isDistinct return true when obj and lastObj are the same, false otherwise
 */
fun <T> LiveData<T>.distinctBy(isDistinct: ((obj: T?, lastObj: T?) -> Boolean)): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()
    distinctLiveData.addSource(this, object : Observer<T> {
        private var initialized = false
        private var lastObj: T? = null
        override fun onChanged(obj: T?) {
            if (!initialized) {
                initialized = true
                lastObj = obj
                distinctLiveData.postValue(lastObj)
            } else if (isDistinct(obj, lastObj)) {
                lastObj = obj
                distinctLiveData.postValue(lastObj)
            }
        }
    })
    return distinctLiveData
}

/**
 * Used in similar use-cases as SingleLiveEvent where you need to monitor state changes to trigger
 * updates. Takes on LiveDate as source but will emit a Pair of the new and previous value
 */
fun <T> createPrevValueLiveData(ld: LiveData<T>): LiveData<Pair<T?, T?>> =
    MediatorLiveData<Pair<T?, T?>>().apply {
        var pastValue: T? = null
        addSource(ld) {
            value = Pair(pastValue, it)
            pastValue = it
        }
    }

/**
 * Allows you to map LiveData values without having to deal with nullability. Ex:
 * val games = MutableLiveData<List<Game>>()
 * val nullableLastGame: LiveData<Game?> = game.map { game.firstOrNull() }
 * val lastGame: LiveData<Game> = game.mapNotNull { game.firstOrNull() }
 */
fun <T, K> LiveData<T>.mapNotNull(mapFun: (T) -> K?): LiveData<K> {
    val mediatorLiveData = MediatorLiveData<K>()
    mediatorLiveData.addSource(this) {
        mapFun(it)?.let { nonNullValue -> mediatorLiveData.value = nonNullValue }
    }
    return mediatorLiveData
}

/** See #createPrevValueLiveData **/
fun <T> LiveData<T>.withPrevValue(): LiveData<Pair<T?, T?>> = createPrevValueLiveData(this)

/** Observe only non-null values */
fun <T> LiveData<T>.observeNonNull(owner: LifecycleOwner, onChanged: (t: T) -> Unit) {
    observe(owner, Observer { it?.let(onChanged) })
}
