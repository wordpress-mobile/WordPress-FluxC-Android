package org.wordpress.android.fluxc.release.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import org.junit.Assert.assertEquals

/**
 * A simple interface to make testing observed values easier.
 */
internal interface ObservedValue<T> {
    val value: T
}

/**
 * A helper function that compares the observed values with the expected ones until the given list runs out.
 *
 * If the observed event is the same as the last one, it'll be ignored. See [ignoreIfSame] for details.
 *
 * @param expectedValues List of expected values
 * @param assertionMessage Assertion message that'll be used during comparison to generate meaningful errors
 * @param onFinish Callback to be called when all events in the [expectedValues] are observed
 */
internal fun <T, OV : ObservedValue<T>> LiveData<T>.testObservedDistinctValues(
    expectedValues: Iterator<OV>,
    assertionMessage: String,
    onFinish: () -> Unit
) {
    val lifecycle = SimpleTestLifecycle()
    this.ignoreIfSame().observe(lifecycle, Observer { actual ->
        val expected = expectedValues.next().value
        assertEquals(assertionMessage, expected, actual)
        if (!expectedValues.hasNext()) {
            // Destroy the lifecycle so we don't get any more values
            lifecycle.destroy()
            onFinish()
        }
    })
}

/**
 * A helper function that filters out an event if it's the same as the last one.
 */
internal fun <T> LiveData<T>.ignoreIfSame(): LiveData<T> {
    val mediatorLiveData: MediatorLiveData<T> = MediatorLiveData()
    var lastValue: T? = null
    mediatorLiveData.addSource(this) {
        // TODO: What happens if the equals() is not implemented correctly for type T
        if (it != lastValue) {
            lastValue = it
            mediatorLiveData.postValue(it)
        }
    }
    return mediatorLiveData
}
