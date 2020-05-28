package se.codeunlimited.livedata.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import se.codeunlimited.livedata.observeForTesting

class LiveDataKtTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Test
    fun `combine emits non-null pairs of both sources`() {
        val a = MutableLiveData<Boolean>()
        val b = MutableLiveData<Boolean>()
        val combined = a.combineWith(b)

        combined.observeForTesting { assertEquals(null, it) }

        a.value = true
        combined.observeForTesting { assertEquals(null, it) }

        b.value = false
        combined.observeForTesting { assertEquals(Pair(first = true, second = false), it) }

        b.value = null
        combined.observeForTesting { assertEquals(Pair(first = true, second = false), it) }
    }

    @Test
    fun `combine emits non-null triples of both sources`() {
        val a = MutableLiveData<Boolean>()
        val b = MutableLiveData<Boolean>()
        val c = MutableLiveData<Boolean>()
        val combined = a.combineWith(b, c)

        combined.observeForTesting { assertEquals(null, it) }

        a.value = true
        combined.observeForTesting { assertEquals(null, it) }

        b.value = false
        combined.observeForTesting { assertEquals(null, it) }

        c.value = true
        combined.observeForTesting {
            assertEquals(
                Triple(
                    first = true,
                    second = false,
                    third = true
                ), it
            )
        }

        a.value = null
        combined.observeForTesting {
            assertEquals(
                Triple(
                    first = true,
                    second = false,
                    third = true
                ), it
            )
        }
    }

    @Test
    fun `getDistinct emits first value`() {
        val a = MutableLiveData<Int>()
        val distinct = a.getDistinct { _, _ -> false }

        distinct.observeForTesting { assertEquals(null, it) }

        // First value is emitted even though predicate is false
        a.value = 42
        distinct.observeForTesting { assertEquals(42, it) }

        // Initial value is emitted
        MutableLiveData(666)
            .getDistinct { _, _ -> false }
            .observeForTesting { assertEquals(666, it) }
    }

    @Test
    fun `getDistinct emits if predicate is true`() {
        val listRef = emptyList<Int>()
        val a = MutableLiveData(listRef)
        val distinct = a.getDistinct { old, new -> old != new } // distinctUntilChanged

        distinct.observeForTesting { assertEquals(listRef, it) }

        // First value is emitted even though predicate is false
        a.value = emptyList()
        distinct.observeForTesting {
            assertEquals(listRef, it)
            assertTrue(listRef === it) // Original reference maintained
        }
    }

    @Test
    fun `withPastValue emits both previous and new value`() {
        val a = MutableLiveData<Int>()
        val aChanges = a.withPastValue()

        aChanges.observeForTesting { assertEquals(null, it) }

        a.value = 42
        aChanges.observeForTesting { assertEquals(Pair(null, 42), it) }

        a.value = 666
        aChanges.observeForTesting { assertEquals(Pair(42, 666), it) }

        a.value = null
        aChanges.observeForTesting { assertEquals(Pair(666, null), it) }
    }
}

