package se.codeunlimited.livedata.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import se.codeunlimited.livedata.observeForTesting

class TransformationsTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    // Transformations.map

    @Test
    fun `fun fact 1, Transformations map will not map null value of uninitialized LiveData`() {
        val ld = MutableLiveData<Boolean>()
        val ldInverse =
            Transformations.map(ld) { !it } // One might expect this to throw NPE as it can be null ?
        ld.observeForTesting { Assert.assertEquals(null, it) }
        ldInverse.observeForTesting { Assert.assertEquals(null, it) }
    }

    @Test(expected = NullPointerException::class)
    fun `fun fact 2, Transformations map will map null value of initialized LiveData`() {
        val ld = MutableLiveData<Boolean>(null)
        val ldInverse = Transformations.map(ld) { !it } // This throws the NPE as it is null
        ldInverse.observeForTesting { Assert.assertEquals(null, it) }
    }

    @Test
    fun `fun fact 3, Transformations map will of course map non null value of initialized LiveData`() {
        val ld = MutableLiveData(false)
        val ldInverse =
            Transformations.map(ld) { !it } // One might expect this to throw NPE as it can be null ?
        ld.observeForTesting { Assert.assertEquals(false, it) }
        ldInverse.observeForTesting { Assert.assertEquals(true, it) }
    }

    @Test
    fun `Transformations map can map null objects if You handle nullability yourself`() {
        val ld = MutableLiveData<Boolean>()
        val ldInverse =
            Transformations.map(ld) { nullableBoolean: Boolean? -> // There is no lint warning about this but the value can always be null here and it's up to you to handle it
                nullableBoolean?.let { !it }
            }
        ld.observeForTesting { Assert.assertEquals(null, it) }
        ldInverse.observeForTesting { Assert.assertEquals(null, it) }

        // Here AS shows it as it: Boolean! because Transformations.map is written in Java and hasn't specified @Nullable on the input for the transformation
        val ldAltSyntaxInverse = Transformations.map(ld) {
            it?.let { !it }
        }
        ldAltSyntaxInverse.observeForTesting { Assert.assertEquals(null, it) }
    }

    @Test
    fun `fun fact 4, Transformations map will also map subsequent null values of LiveData`() {
        val ld = MutableLiveData(false)
        val ldInverse =
            Transformations.map(ld) { it?.let { !it } } // Nullability must be handled since we're setting the value to null later on
        ld.observeForTesting { Assert.assertEquals(false, it) }
        ldInverse.observeForTesting { Assert.assertEquals(true, it) }

        ld.value = null
        ld.observeForTesting { Assert.assertEquals(null, it) }
        ldInverse.observeForTesting { Assert.assertEquals(null, it) }
    }

    // Transformations.switchMap

    @Test
    fun `Transformations switchMap will also map subsequent null values of LiveData`() {
        val ld = MutableLiveData<Boolean>()
        val ldTrue = MutableLiveData("true")
        val ldFalse = MutableLiveData("false")

        val ldResult = Transformations.switchMap(ld) {
            when (it) {
                true -> ldTrue
                else -> ldFalse
            }
        }
        ld.observeForTesting { Assert.assertEquals(null, it) }
        ldResult.observeForTesting { Assert.assertEquals(null, it) }

        ld.value = true
        ld.observeForTesting { Assert.assertEquals(true, it) }
        ldResult.observeForTesting { Assert.assertEquals("true", it) }

        ld.value = null
        ld.observeForTesting { Assert.assertEquals(null, it) }
        ldResult.observeForTesting { Assert.assertEquals("false", it) }
    }
}