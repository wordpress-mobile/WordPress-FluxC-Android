package org.wordpress.android.fluxc.release.utils

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry

/**
 * A simple helper [LifecycleOwner] implementation to be used in tests.
 *
 * It marks its state as `RESUMED` as soon as it's created.
 */
internal class SimpleTestLifecycle : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    init {
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
        lifecycleRegistry.markState(Lifecycle.State.RESUMED)
    }

    /**
     * A function that marks the lifecycle state to `DESTROYED`.
     */
    fun destroy() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
    }
}
