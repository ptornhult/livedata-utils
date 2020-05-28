package se.codeunlimited.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Observes a [LiveData] until the `block` is done executing and passes value in block
 * This is needed when testing LiveData, or else the LiveData just returns null... :(
 */
fun <T> LiveData<T>.observeForTesting(block: (value: T?) -> Unit) {
    val observer = Observer<T> { }
    try {
        observeForever(observer)
        block(value)
    } finally {
        removeObserver(observer)
    }
}