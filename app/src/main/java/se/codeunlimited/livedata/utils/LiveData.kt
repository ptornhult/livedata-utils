package se.codeunlimited.livedata.utils

import androidx.annotation.MainThread
import androidx.lifecycle.*
import java.util.concurrent.atomic.AtomicBoolean


/** Observe only non-null values */
fun <T> LiveData<T>.observeNonNull(owner: LifecycleOwner, onChanged: (t: T) -> Unit) {
    observe(owner, Observer { it?.let(onChanged) })
}

fun <A, B> combine(a: LiveData<A>, b: LiveData<B>) =
    MediatorLiveData<Pair<A, B>>().apply {
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

fun <A, B, C> combine(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>) =
    MediatorLiveData<Triple<A, B, C>>().apply {
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

fun <A, B> LiveData<A>.combineWith(other: LiveData<B>) = combine(this, other)
fun <A, B, C> LiveData<A>.combineWith(other: LiveData<B>, secondOther: LiveData<C>) =
    combine(this, other, secondOther)

/**
 * Similar to Transformations.distinctUntilChanged but you define the predicate in isDistinct
 * @param isDistinct return true when obj and lastObj are the same, false otherwise
 */
fun <T> LiveData<T>.getDistinct(isDistinct: ((obj: T?, lastObj: T?) -> Boolean)): LiveData<T> {
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
 * NOTE WRITTEN BY ME, COPIED FROM SO ;)
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 *
 * Note that only one observer is going to be notified of changes.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val mPending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, observer)

        // Observe the internal MutableLiveData
        super.observe(owner, Observer<T> { t ->
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }
}

/**
 * Used in similar use-cases as SingleLiveEvent where you need to monitor state changes to trigger
 * updates. Takes on LiveDate as source but will emit a Pair of the new and previous value
 */
fun <T> createPastValueLiveData(ld: LiveData<T>) = MediatorLiveData<Pair<T?, T?>>().apply {
    var pastValue: T? = null
    addSource(ld) {
        value = Pair(pastValue, it)
        pastValue = it
    }
}

fun <T> LiveData<T>.withPastValue() = createPastValueLiveData(this)